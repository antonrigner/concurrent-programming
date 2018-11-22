/* WRITTEN BY TOMAS TAMILINAS 950302-8798 mas14tsz@student.lu.se */

/*	Layout: 
	
		> header consisting of: imports, defines, structs and predefines

		> methods for threads
			> int getPictureClient(struct monitor* mon)
			> int getPackSendImage(struct monitor* mon)

		> thread methods
			> void * outputThreadTask(void * ctx)
			> void * inputThreadTask(void * ctx)
			> void * cameraThreadTask(void * ctx)

		> methods for main
			> void exitHandler(int t)
			> int create_mySocket(int portno)

		> main
			> int main(int argc, char *argv[])

	Functionality:
		
		> output_thread		sends pics
		> input_thread		gets commands "i", "m" and "c"
		> camera_thread		updates pic in internal struct
*/


////////////////////ABOVE WAS COMMENTS////////////////////
/////////////////////BELOW ARE HEADER/////////////////////


/* imports */
#include <stdio.h>
#include <sys/types.h> 
#include <sys/socket.h>
#include <netinet/in.h>
#include <string.h>
#include <unistd.h>
#include <stdlib.h>
#include "camera.h"
#include <pthread.h>
#include <errno.h>
#include <signal.h>
#include <time.h>
#include <math.h>
#include <inttypes.h>


/* defines */
#define BUFSIZE 50000
#define DEBUGMODE

/* reference to java socket for pics */ 
struct pictureClient{
	int  connfd;
	byte sendBuff[BUFSIZE];
	byte* frame_data;
};

/* monitor in C */
struct monitor{
	int mySocket; // my socket FD
	int picChanged; // boolean that its not same pic
	int running; // signal that connected with client
	int movie; // if movie mode
	frame* fr; // picture
	unsigned long lastPicTime; // time in ms since last pic sent to client
};

/* predefined terms */
pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cnd = PTHREAD_COND_INITIALIZER;
camera* cam;
struct pictureClient* pictureClient;
int portno;


//////////////////////ABOVE WAS HEADER//////////////////////
/////////////////BELOW ARE METHODS FOR THREADS//////////////


/* returns client socket FD, 0 if error */
int getPictureClient(struct monitor* mon){

	struct sockaddr_in cli_addr;
	listen(mon->mySocket,5);
	socklen_t clilen = (socklen_t) sizeof(cli_addr);
	int clisockfd = accept(mon->mySocket, (struct sockaddr *)&cli_addr, &clilen);
	
	if(clisockfd < 0){
		printf("ERROR: inputThreadTask>getPictureClient() bad client socket FD\n");
		return 0;
	}

	return clisockfd;
}

/* returns bytes sent to client, 0 if error */
int getPackSendImage(struct monitor* mon){

	// get data
	size_t frame_sz = get_frame_size(mon->fr);
	byte *data = get_frame_bytes(mon->fr);			
	unsigned long long timestamp =  get_frame_timestamp(mon->fr);

	/* make header size 30 */
	byte tempSize[10];
	byte tempStamp[20];

	/* store values in temp byte vectors */
	snprintf(tempSize,sizeof(tempSize),"%d",(int)frame_sz);
	snprintf(tempStamp,sizeof(tempStamp),"%llu",timestamp);
		
	/* get length of the values */
	size_t header_size_Size = strlen(tempSize);
	size_t header_size_Stamp = strlen(tempStamp);

	/* define length of each part */
	int header_size_fixed_Size = 10;
	int header_size_fixed_Stamp = 20;
		
	/* create filling vectors */
	char fixSize[header_size_fixed_Size-header_size_Size+1];
	char fixStamp[header_size_fixed_Stamp-header_size_Stamp+1];

	/* fill vectors with char x */
	memset(fixSize,'x',header_size_fixed_Size-header_size_Size);
	memset(fixStamp,'x',header_size_fixed_Stamp-header_size_Stamp);

	/* set last char as terminating 0 */
	fixSize[header_size_fixed_Size-header_size_Size]='\0';
	fixStamp[header_size_fixed_Stamp-header_size_Stamp]='\0';

	/* print over header to client */
	snprintf(pictureClient->sendBuff,sizeof(pictureClient->sendBuff),
	"%d%s%llu%s",(int)frame_sz,fixSize,timestamp,fixStamp);

	size_t header_size = strlen(pictureClient->sendBuff);
	ssize_t packet_sz = header_size + frame_sz;

	pictureClient->frame_data = pictureClient->sendBuff + header_size;
	memcpy(pictureClient->frame_data, data, frame_sz);

	#ifdef DEBUGMODE
	printf("header size: %d\n", (int)header_size);
	printf("picture length: %d\n", (int)frame_sz);
	#endif	

	/* send data to client */					
	int n = write(pictureClient->connfd,pictureClient->sendBuff,packet_sz);					

	#ifdef DEBUGMODE
	printf("Size sent: %d\n",n);
	printf("Size wanted to send: %d\n",(int)packet_sz);
	#endif	

	if (n < 1){
		printf("ERROR: outputThreadTask>getPackSendImage() bad sending to client\n");
		return 0;
	}

	return n;
}


/////////////////ABOVE WAS METHODS FOR THREADS//////////////
//////////////////////BELOW ARE THREADS/////////////////////


/* output_thread */
void * outputThreadTask(void * ctx){

	/* initialization */
	struct monitor *mon = ctx;
	while(1){

		/* check if client exists */
		pthread_mutex_lock(&mtx);
		while(mon->running != 1){
			pthread_cond_wait(&cnd, &mtx);
		}
		
		/* wait if idle mode */
		while(mon->movie == 0 && (unsigned long)time(NULL)<(mon->lastPicTime+5)){
			pthread_cond_wait(&cnd, &mtx);
		}

		/* wait if not new frame */
		while(mon->picChanged != 1){
			pthread_cond_signal(&cnd);
			pthread_cond_wait(&cnd, &mtx);
		}

		/* OK, do it */
		mon->picChanged = 0;
		mon->lastPicTime = (unsigned long)time(NULL);
		
		/* get, pack and send */
		if(mon->running == 1){
			if (getPackSendImage(mon) == 0){exit(5);}
		}
		pthread_cond_signal(&cnd);
		pthread_mutex_unlock(&mtx);

	/* wait to avoid starvation */
	usleep(1000);
	} // end of while 1
	return 0;
}

/* input_thread */
void * inputThreadTask(void * ctx){

	/* initialization */
	struct monitor *mon = ctx;

	/* working */
	while(1){

		/* reset, connect with new client */
		pthread_mutex_lock(&mtx);
		printf("\nWaiting for client at port %d\n", portno);
		pictureClient = malloc(sizeof(*pictureClient));
		pictureClient->connfd = getPictureClient(mon);
		if(pictureClient->connfd == 0){exit(4);}

		/* signal running */
		printf("Connection established!\n");
		mon->running = 1;
		pthread_cond_signal(&cnd);
		pthread_mutex_unlock(&mtx);

		int runningLocal = 1;
		while(runningLocal == 1){

			/* gets message, process if implemented command */
			char temp[1] = {};
			int messageSize = read(pictureClient->connfd, temp, 1);
			if(messageSize>0){
				switch(temp[0]){
					case 'm':
						pthread_mutex_lock(&mtx);
						mon->movie = 1;
						mon->picChanged = 0;
						pthread_cond_signal(&cnd);
						pthread_mutex_unlock(&mtx);
					break;
					case 'i':
						pthread_mutex_lock(&mtx);
						mon->movie = 0;
						mon->picChanged = 0;
						pthread_cond_signal(&cnd);
						pthread_mutex_unlock(&mtx);
					break;		
					case 'c':
						pthread_mutex_lock(&mtx);
						mon->running = 0;
						mon->picChanged = 0;
						pthread_cond_signal(&cnd);
						pthread_mutex_unlock(&mtx);
						runningLocal = 0;
					break;			
				} // end of switch
			} // end of if
		/* wait to avoid starvation */
		usleep(1000);
		} // end of while running	
	} // end of while 1
	return 0;
}

/* camera_thread task */
void * cameraThreadTask(void * ctx){

	/* initialization */
	struct monitor *mon = ctx;
	/* create cam */
	int first = 1;
	cam = camera_open();

	/* working */
	while(1){

		/* check if client exists */
		pthread_mutex_lock(&mtx);
		while(mon->running != 1){
			mon->picChanged = 0;
			pthread_cond_wait(&cnd, &mtx);
		}
		/* clean, get and set pic */	
		if(!first){ // dont clean first time			
			frame_free(mon->fr);
		}
		first = 0;
		mon->fr = camera_get_frame(cam);
		mon->picChanged = 1;		
		pthread_cond_signal(&cnd);
		pthread_mutex_unlock(&mtx);
		
		
		/* wait to not send pics too frequently */
		usleep(33000);

	} // end of while 1
	return 0;
}


//////////////////////ABOVE WAS THREADS/////////////////////
//////////////////BELOW ARE MAIN FUNCTIONS//////////////////


/* returns my socket FD, 0 if error */
int create_mySocket(int portno){

	/* create socket */		
	int temp = socket(AF_INET, SOCK_STREAM, 0);
	if (temp < 0){
		printf("ERROR: main>create_mySocket() failed to create socket\n");
		return 0;
	}

	/* Initialize socket structure */
	struct sockaddr_in serv_addr;
	bzero((char *) &serv_addr, sizeof(serv_addr));
	serv_addr.sin_family = AF_INET;
	serv_addr.sin_addr.s_addr = INADDR_ANY;
	serv_addr.sin_port = htons(portno);

	/* bind socket to port */
	if (bind(temp, (struct sockaddr *) &serv_addr,sizeof(serv_addr)) < 0){
		printf("ERROR: main>create_mySocket() failed to bind or busy port\n");
		close(temp);
		return 0;
	}
	
	return temp;
}

/* Handle ctrl-c smooth shutdown */
void exitHandler(int t){
	printf("\nHalleluja im dead\n");
	exit(0);
}


//////////////////ABOVE WAS MAIN FUNCTIONS//////////////////
///////////////////////BELOW ARE MAIN///////////////////////


int main(int argc, char *argv[]){

	/* ctrl-c friendly */
	signal(SIGINT, exitHandler);

	/* define portno to client */
	if(argc == 2){
		portno = atoi(argv[1]);
	}else{
		portno = 5001;
	}
	printf("\nSetting %d as port number\n",portno);
	
	/* create my socket */	
	int mySocket = create_mySocket(portno);
	if(mySocket==0){exit(1);}

	/* create monitor */
	struct monitor mon = {mySocket,0,0,0,NULL,0};

	/* create threads*/
	pthread_t output_thread;
	pthread_t input_thread;
	pthread_t camera_thread;

	/* initialize the threads */
	if(pthread_create(&output_thread,NULL,outputThreadTask,&mon)){
		printf("ERROR: main> failed to create output_thread\n");
		exit(2);
	}
	if(pthread_create(&input_thread,NULL,inputThreadTask,&mon)){
		printf("ERROR: main> failed to create input_thread\n");
		exit(3);
	}
	if(pthread_create(&camera_thread,NULL,cameraThreadTask,&mon)){
		printf("ERROR: main> failed to create camera_thread\n");
		exit(4);
	}
	
	/* give one thread go */
	sleep(1);
	/* waits for threads to die */
	pthread_join(output_thread,NULL);
	pthread_join(input_thread,NULL);
	pthread_join(camera_thread,NULL);

	/* close everything, say last goodbyes */
	close(mySocket);
	printf("I AM PRETTY MUCH DEAD, CYA\n");
	return 0;
}

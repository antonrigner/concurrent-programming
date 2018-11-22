/* WRITTEN BY TOMAS TAMILINAS 950302-8798 mas14tsz@student.lu.se */

/*	Layout: 
	
		> header consisting of: imports, defines, structs and predefines

		> methods for threads
			> int getNewPictureClient(struct monitor* mon)
			> int sendMotion()
			> int sendDummy()
			> static int try_accept_motion(struct monitor* mon)

		> thread methods
			> void * inputThreadTask(void * ctx)
			> void * outputThreadTask(void * ctx)
			> void * motionThreadTask(void * ctx)

		> methods for main
			> void exitHandler(int t)
			> int create_mySocket(int portno)
			> int createServerSocket()

		> main
			> int main(int argc, char *argv[])

	Functionality:
		
		> input_thread 		waits for client to close
		> output_thread		sends dummy/motion signal 
		> motion_thread		updates motion by polling from motion server
*/


////////////////////ABOVE WAS COMMENTS////////////////////
/////////////////////BELOW ARE HEADER/////////////////////


/* imports */
#include "server_common.h"

#include <stdio.h>
#include <sys/types.h> 
#include <strings.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <signal.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h>
#include <pthread.h>
#include <poll.h>
#include <time.h>

/* defines */
#define MOTION_PORT 9090
#define DEBUGMODE


/* reference to java socket for signal */
struct pictureClient{
	int connfd;
};

/* monitor */
struct monitor{
	int mySocket; 
	int motionChanged; 
	int running; 
	int motionfd;
	int motionfdResponse; 
	int portno;
	struct pollfd motionpollfd;
};

/* predefined terms */
pthread_mutex_t mtx = PTHREAD_MUTEX_INITIALIZER;
pthread_cond_t cnd = PTHREAD_COND_INITIALIZER;
struct pictureClient* pictureClient;


//////////////////////ABOVE WAS HEADER//////////////////////
/////////////////BELOW ARE METHODS FOR THREADS//////////////


/* returns client socket FD, 0 if error */
int getNewPictureClient(struct monitor* mon){	

	struct sockaddr_in cli_addr;
	listen(mon->mySocket,5);	
	socklen_t clilen = (socklen_t) sizeof(cli_addr);
	int clisockfd = accept(mon->mySocket, (struct sockaddr *)&cli_addr, &clilen);

	if(clisockfd < 0){
		printf("ERROR: outputThreadTask>getNewPictureClient() bad client socket FD\n");
		return 0;
	}
	return clisockfd;
}

/* send motion detected signal to client, returns 0 if error */
int sendMotion(){

	int n = write(pictureClient->connfd,"m\0",2);
	if(n<1){
		printf("ERROR: outputThreadTask>sendMotion() bad sending to client\n");
		return 0;
	}
	return n;				
}

/* send dummy signal to client, returns 0 if error */
int sendDummy(){

	int n = write(pictureClient->connfd,"u\0",2);
	if(n<1){
		printf("ERROR: outputThreadTask>sendMotion() bad sending to client\n");
		return 0;
	}
	return n;				
}

/* get motion signal from camera server */
static int try_accept_motion(struct monitor* mon){

	int result = 0;
	mon->motionfdResponse = accept(mon->motionfd, (struct sockaddr*)NULL, NULL);
	if(mon->motionfdResponse < 0) {
		result = 1;
	}	
	return result;
}


/////////////////ABOVE WAS METHODS FOR THREADS//////////////
//////////////////////BELOW ARE THREADS/////////////////////


/* input_thread */
void * inputThreadTask(void * ctx){

	/* initialization */
	struct monitor *mon = ctx;

	/* working */
	while(1){

		/* reset, connect with new client */
		pthread_mutex_lock(&mtx);
		printf("\nWaiting for client at port %d\n", mon->portno);
		pictureClient = malloc(sizeof(*pictureClient));
		pictureClient->connfd = getNewPictureClient(mon);
		if(pictureClient->connfd == 0){exit(4);}

		/* signal running */
		printf("Connection established!\n");
		mon->running = 1;
		pthread_cond_signal(&cnd);
		pthread_mutex_unlock(&mtx);

		int runningLocal = 1;
		while(runningLocal == 1){
			
			/* gets message, process if 'c' */
			char temp[1] = {};
			int messageSize = read(pictureClient->connfd, temp, 1);
			if(messageSize>0 && temp[0] == 'c'){
				pthread_mutex_lock(&mtx);
				mon->running = 0;
				pthread_cond_signal(&cnd);
				pthread_mutex_unlock(&mtx);
				runningLocal = 0;
			}
		/* wait to avoid starvation */
		usleep(1000);
		} // end of while running
		free(pictureClient);	
	} // end of while 1
	return 0;
}

/* output_thread */
void * outputThreadTask(void * ctx){
			
	/* initialization */
	struct monitor *mon = ctx;
	
	/* working */
	while(1){

		/* check if client exists */
		pthread_mutex_lock(&mtx);
		while(mon->running != 1){
			pthread_cond_wait(&cnd, &mtx);
		}

		/* wait for motion */
		while(mon->motionChanged != 1 && mon->running == 1){
			if (sendDummy() == 0){exit(5);}
			pthread_cond_signal(&cnd);
			pthread_cond_wait(&cnd, &mtx);
		}
			
		/* Ok, do it */
		mon->motionChanged = 0;
		if(mon->running == 1){
				#ifdef DEBUGMODE
				printf("DETECTED AND SENDING\n");
				#endif
			if (sendMotion() == 0){exit(5);}
		}else{
			#ifdef DEBUGMODE
			printf("DETECTED\n");
			#endif
		}

		pthread_cond_signal(&cnd);
		pthread_mutex_unlock(&mtx);

	/* wait to avoid starvation */
	usleep(1000);
	} // while 1	
	return 0;	
}

/* motion_thread */
void * motionThreadTask(void * ctx){

	/* initialization */
	struct monitor *mon = ctx;
	mon->motionpollfd.fd = mon->motionfd;
	mon->motionpollfd.events = POLLIN;
	int ret;

	/* working */
	
	while(1){

		/* check if alive */		
		pthread_mutex_lock(&mtx);
				while(mon->running != 1){
					mon->motionChanged = 0;
					pthread_cond_wait(&cnd,&mtx);
				}
		pthread_mutex_unlock(&mtx);

		/* poll for motion */		
		do{
			ret = poll(&mon->motionpollfd,POLLIN,200);
		}while(ret==0);

		if(ret<0){ 		// filter out bad answers from motionfd
			printf("ERROR: motionthread read bad signal from camera\n");
		}else{
			if(try_accept_motion(mon)){ // filter out bad reading of response adress
				printf("ERROR: motionthread>try_accept_motion() failed accept\n");
			}else{
				if((close(mon->motionfdResponse)) ) {
					printf("ERROR: motionthread>try_accept_motion() failed response\n");
			}

				/* if still alive, set motion */
				pthread_mutex_lock(&mtx);
				if(mon->running == 1){
					mon->motionChanged = 1;
				}
				pthread_cond_signal(&cnd);
				pthread_mutex_unlock(&mtx);
			} // else				
		} // else
		/* wait to avoid starvation */
		usleep(1000);
	} // while 1
	return 0;
} 


//////////////////////ABOVE WAS THREADS/////////////////////
//////////////////BELOW ARE MAIN FUNCTIONS//////////////////


/* Handle ctrl-c smooth shutdown */
void exitHandler(int t){
	printf("\nHalleluja im dead\n");
	exit(0);
}

/* returns my socket fd, 0 if error */
int create_mySocket(int portno){

	/* create my socket */		
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

/* returns motion server fd, 0 if error */
int createServerSocket(){

	/* create motion socket */
	int motionfd = socket(AF_INET, SOCK_STREAM, 0);
	if(motionfd < 0){
		printf("ERROR: main>connectServer() failed motionsocket create\n");
		return 0;
	}
	
	int reuse = 1;
	if(setsockopt(motionfd, SOL_SOCKET, SO_REUSEADDR, &reuse, sizeof(reuse))){
		printf("ERROR: main>connectServer() failed motionsocket setsockopt\n");
		return 0;
	}

	/* Initialize socket structure */
	struct sockaddr_in temp;
	int portno = MOTION_PORT;

	memset(&temp, 0, sizeof(temp));
	temp.sin_family = AF_INET;
	temp.sin_addr.s_addr = htonl(INADDR_ANY);
	temp.sin_port = htons(portno);

	/* bind motion socket to port */
	if (bind(motionfd, (struct sockaddr*)&temp, sizeof(temp))) {
		printf("ERROR: main>connectServer() failed conn to motion\n");
		return 0;
	}
	
	/* listen mode on that port */
	if(listen(motionfd,10)){
		printf("ERROR: main>connectServer() listen error\n");
		return 0;
	}

	return motionfd;
}


//////////////////ABOVE WAS MAIN FUNCTIONS//////////////////
///////////////////////BELOW ARE MAIN///////////////////////


/* main */
int main(int argc, char *argv[]){

	int portno;
	/* ctrl-c friendly */
	signal(SIGINT, exitHandler);
		
	/* define portno to client if 1 input*/
	if(argc>1){
		portno = atoi(argv[1]);
	}else{
		portno = 13337;
	}
	printf("\nSetting %d as port number\n", portno);

	/* create my socket */
	int mySocket = create_mySocket(portno);
	if(mySocket==0){exit(1);}

	/* create motion server socket */
	int motionfd = createServerSocket();
	if(motionfd==0){exit(1);}

	/* create structs */
	struct pollfd motionpollfd;
	struct monitor mon = {mySocket,0,0,motionfd,0,portno,motionpollfd};

	/* create threads */
	pthread_t input_thread;
	pthread_t output_thread;
	pthread_t motion_thread;	
		
	/* initialize the threads */
	if(pthread_create(&input_thread,NULL,inputThreadTask,&mon)){
		printf("ERROR: main> failed to create input_thread\n");
		exit(2);
	}
	if(pthread_create(&output_thread,NULL,outputThreadTask,&mon)){
		printf("ERROR: main> failed to create output_thread\n");
		exit(2);
	}

	if(pthread_create(&motion_thread,NULL,motionThreadTask,&mon)){
		printf("ERROR: main> failed to create motion_thread\n");
		exit(2);
	}

	/* give threads go */
	sleep(1);
	
	/* waits for threads to die */
	pthread_join(input_thread,NULL);
	pthread_join(output_thread,NULL);
	pthread_join(motion_thread,NULL);
		
	/* close everything, say last goodbyes */
	close(mySocket);
	printf("I AM PRETTY MUCH DEAD, CYA\n");
	return 0;
}

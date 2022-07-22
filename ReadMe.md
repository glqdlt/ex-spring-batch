
https://docs.spring.io/spring-batch/docs/current/reference/html/spring-batch-intro.html#spring-batch-intro

스프링 배치는 큰 틀에서 보면 배치는 아래의 개념들로 이루어져있다.


- JobRunner : 잡런처 트리거(HTTP 트리거, 스케줄링 트리거, 메세지 브로커 트리거..)

- JobLauncher : 잡의 실행 주체, 잡의 실행 전략(복수 잡 실행, 단일 잡 실행 등..)

- Job : STEP 들의 묶음
    - Step : 배치 작업
        - Reader
        - Processor
        - Writer
    


바텀-업 으로 하나하나 살펴보자.

Reader 는 일을 해야할 대상(엑셀, 또는 DB 레코드들)을 어떻게 획득(GET) 해야할 지를 담당한다.

Processor 는 Reader 가 획득해온 대상에 실제 어떠한 일을 할 지에 대해서 기재한다. Processor 는 여러개가 될 수 있다.  

Writer 는 Processor 가 작업한 결과물을 최종적으로 저장하는 역활을 담당한다.   

만약에 Reader 가 엑셀의 내부 행들을 읽어서 RDB에 저장을 해야 한다는 시나리오가 있다고 가정해보자. Reader 는 엑셀의 내부 행들을 읽는 기능을 담당한다. 여기서 Processor 가 하는 일은 엑셀의 행을 RDB에 저장하기 위한 데이터셋(JPA로 치면 엔티티) 로 변환하는 과정의 일을 하게 된다. Writer 는 변환 된 데이터 셋을 RDB에 BULK INSERT 를 하는 등의 역활을 가진다.

STEP 은 Reader, Processor, Writer 를 하나의 꾸러미로 묶은 컴포넌트이다. Reader Processor Writer 는 물리적으로 다르지만, 사실 ItemStream 라는 추상체를 같은 유형이기 때문에 STEP 내부에서는 List<ItemStream> 이라는 엔트리로 다같이 등록이 된다. 이게 무슨말이냐면, setReader() setProcessor() setWriter() 가 아니라.. addAll(reader,processor, writer) 이런식으로 등록이 된다. 이 말의 의미는 reader,processor,writer 라는 일련의 순서가 아닌 processor, writer, reader 의 순서로 남길수도 있다. 하지만 STEP 이 만들어진 단계에서 올바른 순서로 적재가 되기 때문에 이슈는 없다. 

Job 은 여러개의 Step 을 흐름 제어를 위한 그룹이다. 예를 들어 STEP A와 STEP B가 있다면, STEP A가 끝나면 STEP B를 실행해라 라는 개념이다. 참고로 STEP 과 STEP 간의 커플링을 가지면 안 된다. 커플링을 맺어야 하는 경우라면 STEP 내부에 이러한 커플링을 맺는 것을 만들어야 한다.


JobLauncher 는 잡을 실행하는 전략을 담당한다. 특정 하나의 JOB만 실행할지, 여러개의 JOB 을 동시에 실행할지, JOB 을 어떠한 순서대로 실행할지 등을 말이다. 

JobRunner 는 잡 런처를 실행하는 트리거의 역활을 한다. RestfulAPI 의 호출로 잡런처를 실행할지, Message Broker 를 통해 메세지 이벤트를 전달받아서 잡런처를 실행할지를 담당한다.
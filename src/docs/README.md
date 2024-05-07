# 신호등 사이클 예측 테스트
***

## 테스트 실행하기
***
1. ```src/main/resources/data``` 에 위치한 파일을 ```/var/lib/mysql-files``` 경로에 저장합니다.
2. 아래 스크립트를 실행합니다.
```
#신호등 데이터 생성
LOAD DATA INFILE '/var/lib/mysql-files/traffic.csv'
IGNORE
INTO TABLE api.traffic
FIELDS
    TERMINATED BY ','
LINES
    TERMINATED BY '\n'
IGNORE 1 ROWS
(id);

#신호등 사이클 정보 데이터 생성
LOAD DATA INFILE '/var/lib/mysql-files/trafficCycle.csv'
IGNORE
INTO TABLE api.traffic_cycle
FIELDS
    TERMINATED BY ','
LINES
    TERMINATED BY '\n'
IGNORE 1 ROWS
(traffic_id, red_cycle, green_cycle);

#신호등 정보 호출 최초 1회 데이터 생성
LOAD DATA INFILE '/var/lib/mysql-files/firstCall.csv'
IGNORE
INTO TABLE api.traffic_api_call
FIELDS
    TERMINATED BY ','
LINES
    TERMINATED BY '\n'
IGNORE 1 ROWS
(traffic_id, color, time_left, execution_number);
```

>! (주의) 스크립트 실행 뿐만 아니라 직접 테스트 데이터를 만들어야 합니다. !
>
`TrafficApiCallServiceTest.java` 파일의 `executionNumber` 변수를 직접 1씩 늘려가며 데이터를 생성하여야 합니다.
firstCall.csv 파일에 초기 테스트 데이터가 내장되어 있어 최초 실행 시, `executionNumber` 는 2부터 설정하여 실행시켜야 합니다.
최종적으로 `executionNumber = 20` 까지 실행하는 것을 권장합니다.

`TrafficCyclePredictServiceTest.java` 파일로 신호등 사이클 예측에 대한 테스트를 수행해볼 수 있습니다.

## 테스트 DB Table 설명
***
테스트를 위해 필요한 테이블은 `traffic`, `traffic_cycle`, `traffic_api_call` 총 3개로 구성됩니다.
### traffic
> 신호등을 나타내는 테이블로 컬럼으로 `id`만을 가집니다.

### traffic_cycle
> 신호등의 사이클을 의미합니다.

이후에 서울교통빅데이터플랫폼에서 데이터를 받아온 척 테스트 데이터를 만들기 위해 사용됩니다.
또한 위에서 만들어진 테스트 데이터를 가지고 사이클을 계산한 결과와 비교하여 일치여부를 판단하기 위해 사용됩니다.

### traffic_api_call
> `traffic_cycle` 을 기반으로 생성된 테스트 데이터

특이사항으로 `execution_number` 필드는 데이터를 받아온 회차를 뜻합니다.
예를 들어 `execution_number` 가 2인 레코드는 "두 번째 api 요청을 통해 받아온 데이터 임"을 의미합니다.

## 사이클 예측 수행 과정
***
1. 예측할 신호등 리스트에 대하여 각 신호등에 대한 `traffic_api_call` 데이터를 x개 만큼씩 가져옵니다.
2. 가져온 데이터를 예측할 신호등 리스트에 존재하는 신호등을 단위로 나눕니다.
3. 각각의 신호등에 대해 사이클 예측을 1회 수행합니다.
4. 예측이 추가로 필요한 신호등에 대해 다시 (3)번 과정을 수행합니다.
   4.1. 예측이 필요한 신호등이 없으면 예측한 결과를 반환합니다.
   4.2. (3)번과 (4)번 과정을 반복하며 가지고 있는 모든 데이터에 대해 수행했음에도 예측이 불가능하면 예측이 불가능한 데이터로 판단합니다.


## 테스트 결과
***
* 연속된 두 개의 신호등 정보에 대하여 다음과 같은 패턴을 발견하면 신호등의 사이클을 계산할 수 있습니다.
  * 이전과 이후 신호등 정보에 대해 신호등의 색상이 달라진 경우
  * `after 신호 상태 잔여시간 + (스케줄러 주기 - before 신호 상태의 잔여시간)` 식을 통하여 after 색상의 신호등 사이클을 계산할 수 있습니다.
* ```(빨간불 사이클 + 초록불 사이클) == 스케줄러의 주기``` 인 경우에는 신호등의 사이클을 계산할 수 없다.
* 부동 소수점 연산(?)으로 인해 10^-6 정도 오차가 존재 하지만 문제가 되지 않을 것 같음

## 논의할 사항
***
* 사이클 계산이 불가능하다는 것을 어떻게 판단할 지 논의해보고 싶습니다.
  * 현재는 배치 서버가 저장한 데이터의 끝까지 계산해본 후 사이클 계산이 불가능하다고 알려줍니다.
  * 다른 좋은 방법이 있을까요?

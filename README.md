# EyeShopping
##### Android Vision Application (using OpenCV &amp; NAVER Search API Shopping &amp; Google Vision API)

#### A. 개발환경 구축
- git-flow error : https://community.atlassian.com/t5/Git-questions/How-do-I-resolve-the-not-a-gitflow-enabled-repo-error-when/qaq-p/211028
- OpenCV import
  1. Android NDK, CMake, LLDB를 SDK manager를 통해 설치
  2. CMakeLists.txt를 app/ 에 만들어야함 Path 설정 시 \가 아닌 /에 주의
  3. CMakeLists.txt를 CMake 대상 파일로 지정 해야함 (..졸면서 해서 어떤 메뉴인지 정확히 기억이 안남)
  /dev-diary/android-studio-2-2%EC%97%90%EC%84%9C-opencv-3-1-%EC%84%B8%ED%8C%85%ED%95%98%EA%B8%B0
     - 참고자료
       - http://webnautes.tistory.com/1054
       - https://blog.qwaz.io
       
#### B. Core ex. of Object detection algo. (using openCV)
- http://docs.opencv.org/2.4/doc/tutorials/features2d/detection_of_planar_objects/detection_of_planar_objects.html#detectionofplanarobjects
- http://docs.opencv.org/2.4/doc/tutorials/features2d/feature_homography/feature_homography.html#feature-homography : 구체적 코드 예제

<?php
 var_dump($HTTP_RAW_POST_DATA);
 $input = file_get_contents('php://input');
 print("Our input was\n");
 print($input);
 print("\n");

 print("Lets retrieve all the variables submitted to this \n");
 print("script via a GET request:<br>\n");
 foreach($_GET as $key => $value){
     print("$key=$value<br>\n");
 }
flush();
 print("script via a POST request:<br>\n");
 foreach($_POST as $key => $value){
     print("$key=$value<br>\n");
 }
flush();
 print("All ENV request:<br>\n");
 foreach($_ENV as $key => $value){
     print("$key=$value<br>\n");
 }
flush();

 print("All _SERVER request:<br>\n");
 foreach($_SERVER as $key => $value){
     print("$key=$value<br>\n");
 }
flush();

?>

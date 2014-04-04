<?php
 print("Lets retrieve all the variables submitted to this ");
 print("script via a GET request:<br>");
 foreach($_GET as $key => $value){
     print("$key=$value<br>");
 }

 print("script via a POST request:<br>");
 foreach($_POST as $key => $value){
     print("$key=$value<br>");
 }

 print("All ENV request:<br>");
 foreach($_ENV as $key => $value){
     print("$key=$value<br>");
 }

 print("All _SERVER request:<br>");
 foreach($_SERVER as $key => $value){
     print("$key=$value<br>");
 }
?>

<?php

function generateRandomString($length = 10) {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $randomString = '';
    for ($i = 0; $i < $length; $i++) {
        $randomString .= $characters[rand(0, strlen($characters) - 1)];
    }
    return $randomString;
}

$line = generateRandomString(8191) . "\n";
$a = intval($_GET['lines']);

echo md5($line) . "\n";

for ( $i = 0; $i < $a; $i++ ) {
    echo $line;
    ob_flush();
}
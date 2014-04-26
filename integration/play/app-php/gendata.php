<?php

function generateRandomString($length = 10) {
    $characters = '0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ';
    $randomString = '';
    for ($i = 0; $i < $length; $i++) {
        $randomString .= $characters[rand(0, strlen($characters) - 1)];
    }
    return $randomString;
}

$chunk = generateRandomString(8191) . '\n';
$a = intval($_GET['chunks']);

echo md5($chunk) . '\n';

for ( $i = 0; $i < $a; $i++ ) {
    echo $chunk;
    ob_flush();
}
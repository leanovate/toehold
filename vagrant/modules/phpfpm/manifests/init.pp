
class phpfpm {
    package { "nginx":
        ensure => present
    }

    package { "php-fpm":
        ensure => present
    }

    file { "/etc/nginx/conf.d/default.conf":
        ensure => present,
        content => template("phpfpm/default.conf.template"),
        notify => Service["nginx"],
        require => Package["nginx"]
    }

    file { "/etc/php-fpm.d/devel.conf":
        ensure => present,
        content => template("phpfpm/devel.conf.template"),
        notify => Service["php-fpm"],
        require => Package["php-fpm"]
    }

    service { "nginx":
        ensure => running,
        require => Package["nginx"]
    }

    service { "php-fpm":
        ensure => running,
        require => Package["php-fpm"]
    }    
}

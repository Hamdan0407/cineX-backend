import sys

content = open('c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/pom.xml').read()

import re
new_content = re.sub(r'(<artifactId>spring-boot-starter-webmvc</artifactId>\s*</dependency>)',
                     r'\1\n\t\t<dependency>\n\t\t\t<groupId>org.springframework.boot</groupId>\n\t\t\t<artifactId>spring-boot-starter-validation</artifactId>\n\t\t</dependency>',
                     content)

open('c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/pom.xml', 'w').write(new_content)
print("Updated pom.xml")

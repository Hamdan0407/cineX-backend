import os
import re

base_path = 'c:/Users/Hamdaan/Downloads/bookmyshow/bookmyshow/src/main/java/com/bookmyshow/service/'
for filename in os.listdir(base_path):
    if filename.endswith('.java'):
        filepath = os.path.join(base_path, filename)
        with open(filepath, 'r') as f:
            content = f.read()
        
        # Replace occurrences of generic RuntimeException with specific ones
        # and import the specific ones.
        
        exceptions_to_import = set()
        
        new_content = content
        
        # 1. ResourceNotFoundExceptions
        if re.search(r'new RuntimeException\("([^"]*not found.*?)"\)', new_content, re.IGNORECASE):
            exceptions_to_import.add('ResourceNotFoundException')
            # Handle user not found, show not found, seat not found...
            new_content = re.sub(r'new RuntimeException\("([^"]*not found.*?)"\)', r'new ResourceNotFoundException("\1")', new_content, flags=re.IGNORECASE)

        if re.search(r'new RuntimeException\("([^"]*already exists)"\)', new_content, re.IGNORECASE):
            exceptions_to_import.add('ValidationException')
            new_content = re.sub(r'new RuntimeException\("([^"]*already exists)"\)', r'new ValidationException("\1")', new_content, flags=re.IGNORECASE)
            
        if re.search(r'new RuntimeException\("([^"]*Invalid email or password)"\)', new_content, re.IGNORECASE):
            exceptions_to_import.add('ValidationException')
            new_content = re.sub(r'new RuntimeException\("([^"]*Invalid email or password)"\)', r'new ValidationException("\1")', new_content, flags=re.IGNORECASE)

        if re.search(r'new RuntimeException\("Seat already booked"\)', new_content):
            exceptions_to_import.add('SeatAlreadyBookedException')
            new_content = re.sub(r'new RuntimeException\("Seat already booked"\)', r'new SeatAlreadyBookedException("Seat already booked")', new_content)

        if re.search(r'new RuntimeException\("No seats selected"\)', new_content):
            exceptions_to_import.add('ValidationException')
            new_content = re.sub(r'new RuntimeException\("No seats selected"\)', r'new ValidationException("No seats selected")', new_content)

        # PaymentFailedException isn't actively thrown yet, but maybe we should throw it in BookingService if paymentResponse != SUCCESS? The prompt asks to handle PaymentFailedException, but payment failure just logs to failed booking. We can refactor BookingService slightly or just have it.
        # Actually it says "Replace all manual error checks in services with these custom exceptions".
        
        # Handle "Only CONFIRMED bookings can be cancelled"
        if re.search(r'new RuntimeException\("Only CONFIRMED bookings can be cancelled"\)', new_content):
            exceptions_to_import.add('ValidationException')
            new_content = re.sub(r'new RuntimeException\("Only CONFIRMED bookings can be cancelled"\)', r'new ValidationException("Only CONFIRMED bookings can be cancelled")', new_content)

        import_lines = "".join([f"import com.bookmyshow.exception.{exc};\n" for exc in exceptions_to_import])
        if import_lines:
            # find last import
            match = re.search(r'(import .*?;[\r\n]+)(?!import )', new_content)
            if match:
                new_content = new_content[:match.end()] + import_lines + new_content[match.end():]
            else:
                new_content = re.sub(r'(package com.bookmyshow.service;[\r\n]+)', r'\1\n' + import_lines, new_content)
        
        if new_content != content:
            with open(filepath, 'w') as f:
                f.write(new_content)
            print(f"Refactored {filename}")

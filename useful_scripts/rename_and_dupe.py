import os
import shutil
import sys

# Function to add leading zeros to hour and minute
def add_leading_zeros(hour, minute):
    return f"{hour:02d}{minute:02d}"

# Function to convert 24-hour format to 12-hour format
def convert_to_12_hour(hour):
    if hour == 0:
        return 12
    elif hour <= 12:
        return hour
    else:
        return hour - 12

def main(folder_name, mode):
    source_folder = folder_name
    destination_folder = os.path.join(source_folder, 'renamed')

    # Create the destination folder if it doesn't exist
    if not os.path.exists(destination_folder):
        os.makedirs(destination_folder)

    if mode == "12hr":
        end_hour = 12  # Stop at 11:59 in 12-hour mode
    elif mode == "24hr":
        end_hour = 24
    elif mode == "12to24":
        end_hour = 24
    else:
        print("Invalid mode. Please use '12hr', '24hr', or '12to24'.")
        sys.exit(1)



    for hour in range(end_hour):
        for minute in range(60):
            print("{}{}".format(hour, minute))

            fixed_minute="{:02d}".format(minute)


            if mode == "12to24" and hour >= 12:
                fixed_input_hour = "{:02d}".format(hour - 12)
            else:
                fixed_input_hour = "{:02d}".format(hour)

            padded_hour = "{:02d}".format(hour)

            input_filename = "{}{}.jpg".format(fixed_input_hour, fixed_minute)
            output_filename = "c{}{}.jpg".format(padded_hour, fixed_minute)

            print(f"Processing: {input_filename} -> {output_filename}")

            shutil.copy(os.path.join(source_folder, input_filename), os.path.join(destination_folder, output_filename))
        
          #  # Skip processing after 11:59 for 12-hour mode
          #  if mode == "12hr"  and minute > 59:
          #      break
          #
          #  # Adjust hour for 12to24 mode after 11:59
          #  if mode == "12to24" and hour >= 12:
          #      hour -= 12
          #
          #
          #
          #  # Generate input filename
          #  input_filename = f"{add_leading_zeros(hour, minute)}.jpg"
          #
          #  # Generate output filename
          #  if mode == "12to24" and hour == 11 and minute >= 59:
          #      output_hour = 23
          #  else:
          #      output_hour = hour
          #  output_filename = f"c{add_leading_zeros(output_hour, minute)}.jpg"
          #
          #  # Print the filenames as they are processed
          #  print(f"Processing: {input_filename} -> {output_filename}")
          #
          #  # Copy the file to the destination folder
          #  # shutil.copy(os.path.join(source_folder, input_filename), os.path.join(destination_folder, output_filename))

    print("Files moved successfully.")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        print("Error: Please provide both the folder name and the mode (12hr, 24hr, or 12to24).")
        sys.exit(1)

    folder_name = sys.argv[1]
    mode = sys.argv[2]

    main(folder_name, mode)

import os
import sys
from PIL import Image, ImageDraw

def resize_and_convert_to_circle(input_folder):
    output_folder = input_folder + "_output"
    os.makedirs(output_folder, exist_ok=True)  # Create output folder if it doesn't exist

    time_counter = 0

    for root, _, files in os.walk(input_folder):
        for filename in files:
            if filename.lower().endswith((".png", ".jpg")):
                input_path = os.path.join(root, filename)
                
                # Convert time counter to HHMM format
                hours = time_counter // 60
                minutes = time_counter % 60
                time_str = f"{hours:02d}{minutes:02d}"
                
                output_filename = f"{time_str}.jpg"
                output_path = os.path.join(output_folder, output_filename)

                # Open image and resize
                with Image.open(input_path) as img:
                    # Resize image
                    img = img.resize((396, 396))

                    # Create a new image with black background
                    circle_image = Image.new("RGB", img.size, "black")

                    # Create a mask for the circle
                    mask = Image.new("L", img.size, 0)
                    draw = ImageDraw.Draw(mask)
                    draw.ellipse((0, 0, img.width, img.height), fill=255)

                    # Paste the resized image inside the circle
                    circle_image.paste(img, (0, 0), mask)

                    # Save the resulting image
                    circle_image.save(output_path)

                print(f"Processed: {input_path} -> {output_path}")

                # Increment time counter
                time_counter += 1

if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("Usage: python convert_for_watch-396.py <folder_name>")
    else:
        folder_variable_name = sys.argv[1]
        resize_and_convert_to_circle(folder_variable_name)

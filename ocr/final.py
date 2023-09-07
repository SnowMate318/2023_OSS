from PIL import Image, ImageFilter
from pytesseract import image_to_string
import pytesseract
import cv2
import json
import os

def apply_blur(image, radius):
    blurred_image = image.filter(ImageFilter.GaussianBlur(radius))
    return blurred_image

def add_border(image, border_size):
    width, height = image.size
    new_width = width + 2 * border_size
    new_height = height + 2 * border_size

    new_image = Image.new("RGB", (new_width, new_height), (255, 255, 255))  # 흰색 배경
    new_image.paste(image, (border_size, border_size))

    return new_image

def extract_and_rotate_images(input_path, output_paths_and_coords, blur_strengths, border_sizes):
    image = Image.open(input_path)
    if image is None:
        raise ValueError(f"이미지를 로드할 수 없습니다: {input_path}")

    for output_path, (left, upper, right, lower) in output_paths_and_coords.items():
        cropped_image = image.crop((left, upper, right, lower))

        blur_radius = blur_strengths.get(output_path, 1.5)
        blurred_image = apply_blur(cropped_image, radius=blur_radius)

        border_size = border_sizes.get(output_path, 3)
        bordered_image = add_border(blurred_image, border_size=border_size)

        rotated_image = bordered_image.rotate(-90, expand=True)

        rotated_image.save(output_path)

        text = image_to_string(rotated_image, lang="kor")
        with open(f"sample_{output_path.split('.')[0]}.txt", "w") as f:
            f.write(text)

def start(input_image_path):
    output_paths_and_coords = {
        "output_image_name.jpg": (590, 400, 660, 750),  # 병원이름
        "output_image_day.jpg": (600, 2000, 670, 2330),  # 날짜 l, u, r, l 두번째걸 늘리니까 오른쪽으로 더 짤림, 4번째걸 늘리니까 왼쪽이 더 많이 보임
        "output_image_1_dname.jpg": (1450, 2350, 1550, 2620),  # 약 1
        "output_image_1_dose.jpg": (1450, 1200, 1550, 1345),
        "output_image_1_numb.jpg": (1450, 1100, 1550, 1190),
        "output_image_1_days.jpg": (1450, 1000, 1550, 1050),
        "output_image_2_dname.jpg": (1550, 2435, 1650, 2625),  # 약 2
        "output_image_2_dose.jpg": (1550, 1200, 1650, 1345),
        "output_image_2_numb.jpg": (1550, 1100, 1650, 1190),
        "output_image_2_days.jpg": (1550, 1000, 1650, 1050),
        "output_image_3_dname.jpg": (1650, 2262, 1720, 2488),  # 약 3 세번째걸 늘리면 아래로 더 보임
        "output_image_3_dose.jpg": (1650, 1200, 1750, 1345),
        "output_image_3_numb.jpg": (1650, 1100, 1720, 1190),
        "output_image_3_days.jpg": (1650, 1000, 1750, 1050),
        "output_image_4_dname.jpg": (1750, 2359, 1850, 2630),  # 약 4
        "output_image_4_dose.jpg": (1750, 1200, 1850, 1343),
        "output_image_4_numb.jpg": (1750, 1100, 1850, 1190),
        "output_image_4_days.jpg": (1750, 1000, 1810, 1050),
    }

    blur_strengths = {
        "output_image_name.jpg": 1.5,  # 예시로 강도를 2.0으로 설정
        "output_image_day.jpg": 1.5,
        "output_image_1_dname.jpg": 1.5,  # 약 1
        "output_image_1_dose.jpg": 1.5,
        "output_image_1_numb.jpg": 0,
        "output_image_1_days.jpg": 0,
        "output_image_2_dname.jpg": 1.5,  # 약 2
        "output_image_2_dose.jpg": 1.5,
        "output_image_2_numb.jpg": 1.5,
        "output_image_2_days.jpg": 0,
        "output_image_3_dname.jpg": 1.5,  # 약 3 세번째걸 늘리면 아래로 더 보임
        "output_image_3_dose.jpg": 1.5,
        "output_image_3_numb.jpg": 1,
        "output_image_3_days.jpg": 1.5,
        "output_image_4_dname.jpg": 1.5,  # 약 4
        "output_image_4_dose.jpg": 1.7,
        "output_image_4_numb.jpg": 0,
        "output_image_4_days.jpg": 1.5,
    }

    border_sizes = {
        "output_image_name.jpg": 0,  # 예시로 강도를 2.0으로 설정
        "output_image_day.jpg": 3,
        "output_image_1_dname.jpg": 3,  # 약 1
        "output_image_1_dose.jpg": 3,
        "output_image_1_numb.jpg": 3,
        "output_image_1_days.jpg": 3,
        "output_image_2_dname.jpg": 0,  # 약 2
        "output_image_2_dose.jpg": 3,
        "output_image_2_numb.jpg": 1,
        "output_image_2_days.jpg": 0,
        "output_image_3_dname.jpg": 3,  # 약 3 세번째걸 늘리면 아래로 더 보임
        "output_image_3_dose.jpg": 0,
        "output_image_3_numb.jpg": 3,
        "output_image_3_days.jpg": 3,
        "output_image_4_dname.jpg": 3,  # 약 4
        "output_image_4_dose.jpg": 0,
        "output_image_4_numb.jpg": 3,
        "output_image_4_days.jpg": 3,
    }

    extract_and_rotate_images(input_image_path, output_paths_and_coords, blur_strengths, border_sizes)

    image_paths = [
        '/home/taehyun/output_image_4_days.jpg',
        '/home/taehyun/output_image_4_numb.jpg',
        '/home/taehyun/output_image_3_days.jpg',
        '/home/taehyun/output_image_3_numb.jpg',
        '/home/taehyun/output_image_2_days.jpg',
        '/home/taehyun/output_image_2_numb.jpg',
        '/home/taehyun/output_image_1_days.jpg',
        '/home/taehyun/output_image_1_numb.jpg',
    ]

    # 각 이미지 파일에 대해 반복
    for image_path in image_paths:
        output_path1 = image_path.replace('.jpg', '.txt')
        output_path = output_path1[:14] + "sample_" + output_path1[14:]

        # 이미지를 그레이스케일로 불러옴
        image = cv2.imread(image_path, 0)
        if image is None:
            raise ValueError(f"이미지를 로드할 수 없습니다: {image_path}")

        # 이미지에 Thresholding 적용
        _, thresholded = cv2.threshold(image, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)

        # 이미지에서 텍스트 추출
        text = pytesseract.image_to_string(thresholded, config="--psm 13")

        # 추출된 텍스트를 파일에 저장
        with open(output_path, 'w', encoding='utf-8') as output_file:
            output_file.write(text)

    data = {}

    # 여러 개의 텍스트 파일을 읽어와서 data 리스트에 추가
    file_names = {
        "sample_output_image_1_days.txt", "sample_output_image_1_dname.txt", "sample_output_image_1_dose.txt", "sample_output_image_1_numb.txt",
        "sample_output_image_2_days.txt", "sample_output_image_2_dname.txt", "sample_output_image_2_dose.txt", "sample_output_image_2_numb.txt",
        "sample_output_image_3_days.txt", "sample_output_image_3_dname.txt", "sample_output_image_3_dose.txt", "sample_output_image_3_numb.txt",
        "sample_output_image_4_days.txt", "sample_output_image_4_dname.txt", "sample_output_image_4_dose.txt", "sample_output_image_4_numb.txt",
        "sample_output_image_day.txt", "sample_output_image_name.txt"
    }

    for file_name in file_names:
        try:
            with open(file_name, 'r', encoding='utf-8') as file:
                content = file.read()
                # 줄 바꿈 문자("\n")를 제거
                content = content.replace("\n", "")
                # 데이터를 data 딕셔너리에 추가
                data[file_name] = content
        except:
            with open(file_name, 'r', encoding='ansi') as file:
                content = file.read()
                # 줄 바꿈 문자("\n")를 제거
                content = content.replace("\n", "")
                # 데이터를 data 딕셔너리에 추가
                data[file_name] = content

    # 데이터를 JSON 파일로 저장
    output_file = "output.json"
    with open(output_file, 'w', encoding='utf-8') as json_file:
        json.dump(data, json_file, ensure_ascii=False, indent=4)

    with open("output.json", 'r', encoding='utf-8') as json_file:
        data = json.load(json_file)

    # 키 이름 변경
    key_mapping = {
        "sample_output_image_1_days.txt": "medi_1_totaltake", "sample_output_image_1_dname.txt": "medi_1_med",
        "sample_output_image_1_dose.txt": "medi_1_take_med", "sample_output_image_1_numb.txt": "medi_1_onedaytake",
        "sample_output_image_2_days.txt": "medi_2_totaltake", "sample_output_image_2_dname.txt": "medi_2_med",
        "sample_output_image_2_dose.txt": "medi_2_take_med", "sample_output_image_2_numb.txt": "medi_2_onedaytake",
        "sample_output_image_3_days.txt": "medi_3_totaltake", "sample_output_image_3_dname.txt": "medi_3_med",
        "sample_output_image_3_dose.txt": "medi_3_take_med", "sample_output_image_3_numb.txt": "medi_3_onedaytake",
        "sample_output_image_4_days.txt": "medi_4_totaltake", "sample_output_image_4_dname.txt": "medi_4_med",
        "sample_output_image_4_dose.txt": "medi_4_take_med", "sample_output_image_4_numb.txt": "medi_4_onedaytake",
        "sample_output_image_day.txt": "startmed", "sample_output_image_name.txt": "hospital"
    }

    # 기존 키 이름을 새로운 키 이름으로 변경
    for old_key, new_key in key_mapping.items():
        if old_key in data:
            data[new_key] = data.pop(old_key)

    # 변경된 데이터를 JSON 파일로 저장
    output_file = "output_with_custom_keys.json"
    with open(output_file, 'w', encoding='utf-8') as json_file:
        json.dump(data, json_file, ensure_ascii=False, indent=4)

    print("이미지 편집, 회전, 텍스트 추출 및 저장이 완료되었습니다.")
    return data
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "app" / "src" / "main" / "res" / "drawable-nodpi"
QA = ROOT / "docs" / "qa"
CELL = 64
SCALE = 2
FRAMES = 8
STATES = [
    "idle", "walk", "drag", "pat", "jump", "dance", "eat",
    "sleep_enter", "sleep", "wake", "build", "happy",
]

OUTLINE = (84, 50, 38, 255)
FUR = (220, 153, 78, 255)
FUR_LIGHT = (245, 190, 111, 255)
BELLY = (255, 232, 188, 255)
EAR = (238, 135, 130, 255)
EYE = (48, 35, 39, 255)
CHEEK = (239, 112, 112, 255)
PAW = (248, 205, 145, 255)


def ellipse(draw, box, fill, outline=OUTLINE, width=2):
    draw.ellipse(box, fill=fill, outline=outline, width=width)


def line(draw, points, fill=OUTLINE, width=2):
    draw.line(points, fill=fill, width=width)


def face(draw, x, y, blink=False, happy=False, chew=False):
    if blink or happy:
        line(draw, [(x - 9, y), (x - 6, y + (1 if happy else 0))], width=2)
        line(draw, [(x + 6, y + (1 if happy else 0)), (x + 9, y)], width=2)
    else:
        draw.rectangle((x - 9, y - 1, x - 7, y + 2), fill=EYE)
        draw.rectangle((x + 7, y - 1, x + 9, y + 2), fill=EYE)
        draw.point((x - 8, y - 1), fill=(255, 255, 255, 255))
        draw.point((x + 8, y - 1), fill=(255, 255, 255, 255))
    draw.rectangle((x - 1, y + 3, x + 1, y + 4), fill=(119, 64, 66, 255))
    if chew:
        draw.rectangle((x - 1, y + 6, x + 1, y + 8), fill=EYE)
    else:
        line(draw, [(x - 3, y + 6), (x, y + 7), (x + 3, y + 6)], width=1)
    draw.rectangle((x - 13, y + 3, x - 11, y + 5), fill=CHEEK)
    draw.rectangle((x + 11, y + 3, x + 13, y + 5), fill=CHEEK)


def upright(frame, state, dy=0, dx=0, squat=0):
    image = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    blink = state == "idle" and frame in (3, 4)
    happy = state in ("pat", "dance", "happy")
    chew = state == "eat" and frame % 2 == 1
    leg_phase = frame % 4
    body_top = 23 + dy + squat
    body_bottom = 58 + dy
    head_y = 11 + dy + squat // 2
    head_bottom = 39 + dy + squat // 2
    x0 = 17 + dx
    x1 = 49 + dx

    if state == "jump":
        paw_raise = frame in (2, 3, 4, 5)
    else:
        paw_raise = state in ("dance", "happy", "build")

    ear_drop = 2 if state == "pat" and frame in (2, 3, 4, 5) else 0
    ellipse(draw, (19 + dx, 7 + dy + ear_drop, 29 + dx, 20 + dy + ear_drop), FUR)
    ellipse(draw, (37 + dx, 7 + dy + ear_drop, 47 + dx, 20 + dy + ear_drop), FUR)
    ellipse(draw, (22 + dx, 10 + dy + ear_drop, 27 + dx, 17 + dy + ear_drop), EAR, width=1)
    ellipse(draw, (39 + dx, 10 + dy + ear_drop, 44 + dx, 17 + dy + ear_drop), EAR, width=1)
    ellipse(draw, (x0, body_top, x1, body_bottom), FUR)
    ellipse(draw, (23 + dx, 30 + dy + squat, 43 + dx, 56 + dy), BELLY, outline=FUR_LIGHT, width=1)
    ellipse(draw, (16 + dx, head_y, 50 + dx, head_bottom), FUR_LIGHT)

    if state == "walk":
        foot_left = -3 if leg_phase in (0, 1) else 2
        foot_right = 3 if leg_phase in (0, 1) else -2
    elif state == "drag":
        foot_left = -2 if leg_phase in (0, 1) else 1
        foot_right = 2 if leg_phase in (0, 1) else -1
    else:
        foot_left = foot_right = 0
    ellipse(draw, (18 + dx + foot_left, 53 + dy, 31 + dx + foot_left, 61 + dy), PAW)
    ellipse(draw, (35 + dx + foot_right, 53 + dy, 48 + dx + foot_right, 61 + dy), PAW)

    if state == "drag":
        arm_y = 33 + (frame % 2)
        ellipse(draw, (39 + dx, arm_y + dy, 57 + dx, arm_y + 7 + dy), PAW)
        ellipse(draw, (37 + dx, arm_y + 7 + dy, 54 + dx, arm_y + 13 + dy), PAW)
    elif state == "eat":
        offset = 1 if frame % 2 else 0
        ellipse(draw, (20 + dx, 37 + dy + offset, 32 + dx, 48 + dy + offset), PAW)
        ellipse(draw, (34 + dx, 37 + dy + offset, 46 + dx, 48 + dy + offset), PAW)
    elif paw_raise:
        left_up = frame % 4 < 2
        ellipse(draw, (13 + dx, (25 if left_up else 37) + dy, 24 + dx, (39 if left_up else 48) + dy), PAW)
        ellipse(draw, (42 + dx, (37 if left_up else 25) + dy, 53 + dx, (48 if left_up else 39) + dy), PAW)
    else:
        arm_shift = 2 if state == "walk" and frame % 2 else 0
        ellipse(draw, (15 + dx, 35 + dy + arm_shift, 27 + dx, 47 + dy + arm_shift), PAW)
        ellipse(draw, (39 + dx, 35 + dy - arm_shift, 51 + dx, 47 + dy - arm_shift), PAW)

    face(draw, 33 + dx, 25 + dy + squat // 2, blink=blink, happy=happy, chew=chew)

    if state == "build":
        straw_y = 29 + (frame % 3) * 2
        line(draw, [(43 + dx, straw_y + dy), (57 + dx, straw_y + 10 + dy)], fill=(236, 191, 72, 255), width=3)
        line(draw, [(43 + dx, straw_y + dy), (57 + dx, straw_y + 10 + dy)], fill=(130, 92, 37, 255), width=1)
    return image


def sleeping(frame, entering=False, waking=False):
    image = Image.new("RGBA", (CELL, CELL), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if entering or waking:
        phase = frame if entering else 7 - frame
        if phase < 3:
            return upright(frame, "idle", dy=phase * 2, squat=phase * 2)
    breath = 1 if frame in (2, 3, 4) else 0
    ellipse(draw, (8, 35 - breath, 48, 57), FUR)
    ellipse(draw, (12, 40 - breath, 39, 56), BELLY, outline=FUR_LIGHT, width=1)
    ellipse(draw, (37, 31 - breath, 59, 51), FUR_LIGHT)
    ellipse(draw, (43, 27 - breath, 52, 38), FUR)
    ellipse(draw, (45, 29 - breath, 50, 35), EAR, width=1)
    line(draw, [(43, 41 - breath), (48, 41 - breath)], width=2)
    draw.rectangle((53, 43 - breath, 55, 44 - breath), fill=(119, 64, 66, 255))
    ellipse(draw, (32, 49, 43, 58), PAW)
    return image


def adult_frame(state, frame):
    if state == "idle":
        return upright(frame, state, dy=[0, 0, -1, -1, -1, 0, 1, 0][frame])
    if state == "walk":
        return upright(frame, state, dy=[0, -2, -1, 1, 0, -2, -1, 1][frame], dx=2)
    if state == "drag":
        return upright(frame, state, dy=[0, -1, 0, 1, 0, -1, 0, 1][frame], dx=1, squat=2)
    if state == "pat":
        return upright(frame, state, squat=[0, 2, 5, 7, 7, 5, 2, 0][frame])
    if state == "jump":
        return upright(frame, state, dy=[0, -5, -11, -16, -16, -10, -4, 0][frame])
    if state == "dance":
        return upright(frame, state, dx=[0, -3, -5, -2, 0, 3, 5, 2][frame], dy=[0, -1, 0, 1, 0, -1, 0, 1][frame])
    if state == "eat":
        return upright(frame, state, squat=[0, 1, 0, 2, 0, 1, 0, 2][frame])
    if state == "sleep_enter":
        return sleeping(frame, entering=True)
    if state == "sleep":
        return sleeping(frame)
    if state == "wake":
        return sleeping(frame, waking=True)
    if state == "build":
        return upright(frame, state, dx=[0, 1, 2, 1, 0, -1, -2, -1][frame], squat=2)
    return upright(frame, "happy", dy=[0, -2, -5, -2, 0, -2, -5, -2][frame])


def make_adult_atlas():
    atlas = Image.new("RGBA", (CELL * FRAMES, CELL * len(STATES)), (0, 0, 0, 0))
    for row, state in enumerate(STATES):
        for frame in range(FRAMES):
            atlas.alpha_composite(adult_frame(state, frame), (frame * CELL, row * CELL))
    atlas = atlas.resize((atlas.width * SCALE, atlas.height * SCALE), Image.Resampling.NEAREST)
    atlas.save(RES / "hamster_sprite_atlas.png", optimize=True)
    return atlas


def baby_frame(variant, stage, action="idle", frame=0):
    palettes = [
        ((240, 204, 147, 255), (255, 239, 204, 255), (124, 83, 55, 255)),
        ((173, 181, 188, 255), (248, 247, 239, 255), (73, 78, 83, 255)),
        ((177, 105, 55, 255), (247, 188, 111, 255), (82, 47, 31, 255)),
    ]
    fur, light, outline = palettes[variant]
    image = Image.new("RGBA", (48, 48), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    sizes = [(13, 18, 35, 39), (10, 13, 38, 41), (8, 9, 40, 43)]
    x0, y0, x1, y1 = sizes[stage]
    if action == "idle":
        y_shift = [0, 0, -1, -1, 0, 1, 0, 0][frame]
    elif action == "feed":
        y_shift = [0, 0, 1, 1, 0, 1, 0, 0][frame]
    else:
        y_shift = [0, -2, -5, -7, -5, -2, 0, 0][frame]
    x_shift = [0, -1, -2, -1, 0, 1, 2, 1][frame] if action == "play" else 0
    x0 += x_shift
    x1 += x_shift
    y0 += y_shift
    y1 += y_shift
    draw.ellipse((x0 + 2, y0 - 5, x0 + 11, y0 + 6), fill=fur, outline=outline, width=2)
    draw.ellipse((x1 - 11, y0 - 5, x1 - 2, y0 + 6), fill=fur, outline=outline, width=2)
    draw.ellipse((x0, y0, x1, y1), fill=fur, outline=outline, width=2)
    draw.ellipse((x0 + 6, y0 + 7, x1 - 6, y1 - 1), fill=light)
    eye_y = y0 + 10
    blink = action == "idle" and frame in (3, 4)
    happy = action == "play" and frame in (2, 3, 4, 5)
    if blink or happy:
        line(draw, [(x0 + 7, eye_y + 2), (x0 + 10, eye_y + 1)], fill=outline, width=1)
        line(draw, [(x1 - 10, eye_y + 1), (x1 - 7, eye_y + 2)], fill=outline, width=1)
    elif variant == 1:
        draw.rectangle((x0 + 7, eye_y, x0 + 9, eye_y + 3), fill=outline)
        line(draw, [(x1 - 10, eye_y + 2), (x1 - 7, eye_y)], fill=outline, width=2)
    else:
        draw.rectangle((x0 + 7, eye_y, x0 + 9, eye_y + 2), fill=outline)
        draw.rectangle((x1 - 9, eye_y, x1 - 7, eye_y + 2), fill=outline)
    nose_x = 24 + x_shift
    draw.rectangle((nose_x - 1, eye_y + 4, nose_x + 1, eye_y + 5), fill=(126, 66, 68, 255))
    if variant == 2:
        draw.polygon([(22, y0 + 1), (27, y0 + 1), (25, y0 + 7)], fill=light)
    if stage == 0:
        draw.rectangle((x0 + 5, y1 - 1, x1 - 5, y1 + 1), fill=light)
    elif stage == 2:
        draw.ellipse((x0 + 1, y1 - 5, x0 + 11, y1 + 2), fill=light, outline=outline, width=1)
        draw.ellipse((x1 - 11, y1 - 5, x1 - 1, y1 + 2), fill=light, outline=outline, width=1)

    if action == "feed":
        paw_y = eye_y + 8 + (frame % 2)
        draw.ellipse((nose_x - 9, paw_y, nose_x - 2, paw_y + 6), fill=light, outline=outline, width=1)
        draw.ellipse((nose_x + 2, paw_y, nose_x + 9, paw_y + 6), fill=light, outline=outline, width=1)
        if frame < 6:
            food_radius = 3 if frame < 3 else 2
            draw.ellipse(
                (nose_x - food_radius, paw_y - 2 - food_radius, nose_x + food_radius, paw_y - 2 + food_radius),
                fill=(235, 181, 58, 255),
                outline=(119, 76, 37, 255),
                width=1,
            )
        if frame % 2:
            draw.rectangle((nose_x - 1, eye_y + 7, nose_x + 1, eye_y + 9), fill=outline)
    elif action == "play":
        paw_up = frame in (1, 2, 3, 4)
        paw_y = y0 + (4 if paw_up else 15)
        draw.ellipse((x1 - 6, paw_y, x1 + 2, paw_y + 8), fill=light, outline=outline, width=1)
        ball_x = [7, 11, 16, 22, 28, 34, 39, 42][frame]
        ball_y = [37, 31, 25, 21, 24, 29, 34, 38][frame]
        draw.ellipse((ball_x - 4, ball_y - 4, ball_x + 4, ball_y + 4), fill=(88, 164, 139, 255), outline=outline, width=1)
    return image.resize((96, 96), Image.Resampling.NEAREST)


def make_baby_atlas():
    actions = ["idle", "feed", "play"]
    atlas = Image.new("RGBA", (96 * FRAMES, 96 * len(actions) * 3 * 3), (0, 0, 0, 0))
    for action_index, action in enumerate(actions):
        for stage in range(3):
            for variant in range(3):
                row = action_index * 9 + stage * 3 + variant
                for frame in range(FRAMES):
                    atlas.alpha_composite(baby_frame(variant, stage, action, frame), (frame * 96, row * 96))
    atlas.save(RES / "baby_sprite_atlas.png", optimize=True)


def make_icon():
    icon = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(icon)
    draw.rounded_rectangle((42, 42, 470, 470), radius=92, fill=(223, 244, 239, 255))
    pet = adult_frame("happy", 2).resize((384, 384), Image.Resampling.NEAREST)
    icon.alpha_composite(pet, (64, 76))
    icon.save(RES / "app_icon.png", optimize=True)

    round_icon = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    round_draw = ImageDraw.Draw(round_icon)
    round_draw.ellipse((36, 36, 476, 476), fill=(223, 244, 239, 255))
    round_pet = adult_frame("happy", 2).resize((360, 360), Image.Resampling.NEAREST)
    round_icon.alpha_composite(round_pet, (76, 88))
    round_icon.save(RES / "app_icon_round.png", optimize=True)


def make_contact_sheet(atlas):
    QA.mkdir(parents=True, exist_ok=True)
    cell = 128
    label_width = 130
    sheet = Image.new("RGB", (label_width + atlas.width, atlas.height), (245, 244, 240))
    checker = ImageDraw.Draw(sheet)
    for y in range(0, atlas.height, 16):
        for x in range(label_width, sheet.width, 16):
            color = (223, 226, 224) if ((x // 16 + y // 16) % 2) else (248, 248, 246)
            checker.rectangle((x, y, x + 15, y + 15), fill=color)
    sheet.paste(atlas, (label_width, 0), atlas)
    font = ImageFont.load_default()
    for row, state in enumerate(STATES):
        checker.text((8, row * cell + 54), state, fill=(45, 47, 51), font=font)
    sheet.save(QA / "pixel_hamster_contact_sheet.png", optimize=True)


def main():
    RES.mkdir(parents=True, exist_ok=True)
    adult = make_adult_atlas()
    make_baby_atlas()
    make_icon()
    make_contact_sheet(adult)


if __name__ == "__main__":
    main()

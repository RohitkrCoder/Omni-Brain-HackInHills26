import cv2
from ultralytics import YOLO

MODEL_PATH = "yolov8n.pt"
CAMERA_INDEX = 0
CONF_THRESHOLD = 0.4
AREA_RATIO_THRESHOLD = 0.25

model = YOLO(MODEL_PATH)
cap = cv2.VideoCapture(CAMERA_INDEX)

if not cap.isOpened():
    raise RuntimeError("Cannot open camera")

while True:
    ok, frame = cap.read()
    if not ok:
        break

    frame_h, frame_w = frame.shape[:2]
    frame_area = frame_w * frame_h

    results = model(frame, conf=CONF_THRESHOLD, verbose=False)

    for result in results:
        for box in result.boxes:
            x1, y1, x2, y2 = map(int, box.xyxy[0].tolist())
            cls_id = int(box.cls[0])
            conf = float(box.conf[0])
            label = f"{model.names[cls_id]} {conf:.2f}"

            cv2.rectangle(frame, (x1, y1), (x2, y2), (0, 255, 0), 2)
            cv2.putText(frame, label, (x1, max(y1 - 6, 12)), cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 255, 0), 1)

            box_area = max(0, x2 - x1) * max(0, y2 - y1)
            ratio = box_area / frame_area

            if ratio > AREA_RATIO_THRESHOLD:
                print(f"WARNING: {model.names[cls_id]} occupies {ratio * 100:.1f}% of frame")

    cv2.imshow("YOLOv8n", frame)

    if cv2.waitKey(1) & 0xFF == ord("q"):
        break

cap.release()
cv2.destroyAllWindows()
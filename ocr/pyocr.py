import os
import firebase_admin
from firebase_admin import credentials, firestore, storage

import final
from final import start

import datetime
from datetime import datetime, timedelta
import time
from time import sleep

import threading




cred = credentials.Certificate("/home/taehyun/project/testPrj/prompt_engine/aiproject-8e5e6-firebase-adminsdk-13r8e-4f8b6db6d0.json") 
firebase_admin.initialize_app(cred, {
    'storageBucket': 'gs://aiproject-8e5e6.appspot.com/'
})
db= firestore.client()

bucket= storage.bucket('aiproject-8e5e6.appspot.com')


#capture listener
def on_capture_snapshot(docid, col_snapshot, changes, read_time):
    for change in changes:
        if change.type.name=="ADDED":
            doc_data = change.document.to_dict()
            print("change_success")
            #1. storage에서 사진 가져오기
            image_path = f"{docid}/images"
            
            blob= bucket.blob(image_path)
            
            image_data=blob.download_as_bytes()
            #2.ocr
        
            ocr_result = start("/home/taehyun/sample5.jpeg")
            print(ocr_result)
            #3.ocr 추출물을 firestore에 저장
            user_medication_ref = db.collection("User").document(docid).collection("Medication")
            
            for idx in range(1, (len(ocr_result) - 2) // 4 + 1): # -2는 'hospital'과 'startmed'를 뺀 개수
                medi_prefix = f"medi_{idx}_"
                
                medi_name = ocr_result.get(medi_prefix + "med", 0)
                one_day = ocr_result.get(medi_prefix + "onedaytake")
                if one_day is not None:
                    try:
                        one_day = int(one_day)
                    except (ValueError, TypeError):
                        one_day = 0
                else:
                    one_day = 0
                
                
                total_take_str = ocr_result.get(medi_prefix + "totaltake")
                try:
                    total_take=int(total_take_str)
                except:
                    total_take=0
                
                
                startmed = ocr_result.get("startmed", 0)
                from datetime import datetime
                try:
                    start_date = datetime.strptime(startmed, '%y-%m-%d')
                except ValueError:
                    start_date = datetime.today()

                end_date = start_date + timedelta(days=total_take)
                
                hospital = ocr_result.get("hospital", "")
                
                medication_data = {
                    "medi_name": medi_name,
                    "one_day": one_day,
                    #"start_date": start_date,
                    "end_date": end_date,
                    "hospital_name": hospital,
                    "enable": True
                    
                }
                
                #user_medication_ref.add(medication_data)
                db.collection("User").document(docid).collection("Medication").add(medication_data)
        
            db.collection("User").document(docid).collection("Medication").add(medication_data)
            
            #4. delete storage
            blob.delete()
            
            #5. delete capture
            #db.collection("User").document(docid).collection("Capture").delete()


            


def on_snapshot(col_snapshot, changes, read_time):
    
    for change in changes: 
        print("Change object:", change)
        print("Change type:", change.type)
        if change.type.name== "ADDED": #추가된게 있으면 -> 새로운 어떤 회원이 새로 등록 하였을 경우
            doc = change.document
            docid = doc.id # 그 새로운 회원의 uid를 따내서
            print(docid)
            if docid not in query_watches:
                col_query = db.collection("User").document(docid).collection("Capture") # Message 
                query_watch = col_query.on_snapshot(lambda col_snapshot, changes, read_time: on_capture_snapshot(docid, col_snapshot, changes, read_time)) # 
                query_watches[docid] = query_watch
    

# ...

if __name__ == "__main__":
    query_watches = {}
    col_query_user = db.collection("User") 
    query_watch_user = col_query_user.on_snapshot(on_snapshot) 

    try:
        print("Program Is Running")
        while True:
            time.sleep(10)
    except KeyboardInterrupt: 
        pass
    finally:
        query_watch_user.unsubscribe() 
        for doc_id, query_watch in query_watches.items(): 
            query_watch.unsubscribe()
        print("Unsubscribed from all watches")
        
        for thread in threading.enumerate():
            if thread is not threading.currentThread():
                print(f"Waiting for thread {thread.name} to exit...")
                thread.join()
        print("All threads have exited.")

            

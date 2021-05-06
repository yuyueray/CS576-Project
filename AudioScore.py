# CS576 Project
# Yue Yu, Spring 2021
from scipy.io import wavfile
import numpy as np    

# import time
# import matplotlib.pyplot as plt

# to run this file in terminal
# >> python AudioScore.py
# >> filePath

# returns the score[0-10] for a wav file by seconds  
# the result is saved to filePath_audio_score.txt, one row is one score value 
def getAudioScore(filePath):
    # print(time.ctime(time.time()))
    samplerate, data = wavfile.read(filePath)
    monoSound = np.array([l if l >= 0 else 0 for l, r in data])
    sampleNum = len(monoSound)
    secLength = sampleNum // samplerate
    lastLeftNum = sampleNum % samplerate
    monoSound = monoSound[:-lastLeftNum]
    secPCM = np.mean(monoSound.reshape(-1, samplerate), axis=1)
    secPCM = np.insert(secPCM, 0,secPCM[0])
    score = [0]
    for i in range(secLength):
        increase = (secPCM[i + 1] - secPCM[i]) / secPCM[i]
        lastScore = score[-1]
        curScore = lastScore + increase * 100 // 10
        if curScore > 10:
            curScore = 10
        elif curScore < 0:
            curScore = 0
        score.append(curScore)
    del score[0]
    scoreFile = filePath + '_audio_score.txt'
    with open(scoreFile, 'w+') as audioScoreFile:
        for s in score:
            audioScoreFile.write('%s\n' % s)
    print('AudioScore is saved at: ' + scoreFile)
    return

# getAudioScore('/Users/ray/Desktop/CS576/project_dataset/audio/concert.wav')

if __name__ == '__main__':
    fPath = str(input())
    print('The path is: ' + fPath)
    getAudioScore(fPath)

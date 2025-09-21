(ns com.badlogic.gdx.backends.lwjgl3.init.gdx
  (:import (com.badlogic.gdx Gdx)))

(defn set-app! [{:keys [init/application] :as init}]
  (set! Gdx/app application)
  init)

(defn set-audio! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
                   :as init}]
  (set! Gdx/audio (.audio application))
  init)

(defn set-files! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
                   :as init}]
  (set! Gdx/files (.files application))
  init)

(defn set-net! [{:keys [^com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application init/application]
                 :as init}]
  (set! Gdx/net (.net application))
  init)

(ns clojure.gdx.backends.lwjgl
  (:require [clojure.gdx.audio]
            [com.badlogic.gdx :as gdx]
            [com.badlogic.gdx.audio :as audio]
            [com.badlogic.gdx.files :as files]
            [com.badlogic.gdx.application.listener :as listener]
            [com.badlogic.gdx.backends.lwjgl3.application :as application]
            [com.badlogic.gdx.backends.lwjgl3.application.config :as config]))

(defrecord Context []
  clojure.gdx.audio/Audio
  (sound [{:keys [clojure.gdx/audio
                  clojure.gdx/files]}
          path]
    (audio/sound audio (files/internal files path))))

(defn create [{:keys [create] :as listener} config]
  (application/create (listener/create
                       (assoc listener :create
                              (fn []
                                (create (map->Context
                                         (let [{:keys [audio files graphics input]} (gdx/context)]
                                           {:clojure.gdx/audio    audio
                                            :clojure.gdx/files    files
                                            :clojure.gdx/graphics graphics
                                            :clojure.gdx/input    input}))))))
                      (config/create config)))

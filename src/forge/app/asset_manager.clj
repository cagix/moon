(ns forge.app.asset-manager
  (:require [clojure.string :as str]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.audio.sound :as sound]
            [clojure.gdx.files :as files]
            [clojure.gdx.utils.disposable :refer [dispose]]
            [clojure.utils :refer [bind-root]]))

(declare asset-manager)

(defn create [[_ folder]]
  (bind-root asset-manager (manager/create))
  (manager/load asset-manager
                (for [[asset-type exts] [[:sound   #{"wav"}]
                                         [:texture #{"png" "bmp"}]] ; <- this is also data !
                      file (map #(str/replace-first % folder "")
                                (files/recursively-search folder exts))]
                  [file asset-type])))

(defn destroy [_]
  (dispose asset-manager))

(defn play-sound [sound-name]
  (->> sound-name
       (format "sounds/%s.wav")  ; <- this is also data ! => might the total conversion even be happening!?
       asset-manager
       sound/play))

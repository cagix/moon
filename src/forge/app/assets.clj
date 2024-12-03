(ns ^:no-doc forge.app.assets
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str]
            [forge.system :as system]
            [forge.utils.files :as files]))

(defn- load-assets [folder]
  (let [manager (gdx/asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                          [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (files/recursively-search (gdx/internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :app/assets
  (system/create [[_ folder]]
    (bind-root #'system/assets (load-assets folder)))
  (system/dispose [_]
    (.dispose system/assets)))

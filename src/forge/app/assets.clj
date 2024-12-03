(ns ^:no-doc forge.app.assets
  (:require [clojure.gdx :as gdx]
            [clojure.string :as str]
            [forge.core :refer :all]
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
  (app-create [[_ folder]]
    (bind-root #'assets (load-assets folder)))
  (app-dispose [_]
    (.dispose assets)))

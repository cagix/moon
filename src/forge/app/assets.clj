(ns ^:no-doc forge.app.assets
  (:require [clojure.string :as str]
            [forge.core :refer :all]
            [forge.utils.files :as files])
  (:import (com.badlogic.gdx.assets AssetManager)))

(defn- asset-manager ^AssetManager []
  (proxy [AssetManager clojure.lang.ILookup] []
    (valAt [^String path]
      (if (AssetManager/.contains this path)
        (AssetManager/.get this path)
        (throw (IllegalArgumentException. (str "Asset cannot be found: " path)))))))

(defn- load-assets [folder]
  (let [manager (asset-manager)]
    (doseq [[class exts] [[com.badlogic.gdx.audio.Sound      #{"wav"}]
                          [com.badlogic.gdx.graphics.Texture #{"png" "bmp"}]]
            file (map #(str/replace-first % folder "")
                      (files/recursively-search (internal-file folder) exts))]
      (.load manager ^String file ^Class class))
    (.finishLoading manager)
    manager))

(defmethods :app/assets
  (app-create [[_ folder]]
    (bind-root #'assets (load-assets folder)))
  (app-dispose [_]
    (.dispose assets)))

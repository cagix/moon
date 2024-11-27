(ns app.lifecycle
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.graphics.color :as color]
            [clojure.gdx.utils :as utils :refer [clear-screen]]
            [clojure.string :as str]
            [forge.app :as app]
            [forge.assets :as assets]
            [forge.graphics :as graphics]
            [forge.graphics.cursors :as cursors]
            [forge.stage :as stage]
            [forge.ui :as ui])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.files FileHandle)
           (com.badlogic.gdx.graphics Texture)))

(defn- recursively-search [folder extensions]
  (loop [[^FileHandle file & remaining] (.list (gdx/internal-file folder))
         result []]
    (cond (nil? file)
          result

          (.isDirectory file)
          (recur (concat remaining (.list file)) result)

          (extensions (.extension file))
          (recur remaining (conj result (.path file)))

          :else
          (recur remaining result))))

(defn- search
  "Returns a collection of `[file-path class]` after recursively searching `folder` and matches all `.wav` with `com.badlogic.gdx.audio.Sound` and all `.png`/`.bmp` files with `com.badlogic.gdx.graphics.Texture`."
  [folder]
  (for [[class exts] [[Sound   #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (recursively-search folder exts))]
    [file class]))

(defn create [asset-folder
              cursors
              ui-skin-scale
              screens
              first-screen-k]
  (assets/init (search asset-folder))
  (bind-root #'cursors/cursors (cursors))
  (graphics/init)
  (ui/load! ui-skin-scale)
  (bind-root #'app/screens (mapvals stage/create (screens)))
  (app/change-screen first-screen-k))

(defn dispose []
  (assets/dispose)
  (run! utils/dispose (vals cursors/cursors))
  (graphics/dispose)
  (run! app/dispose (vals app/screens))
  (ui/dispose!))

(defn render []
  (clear-screen color/black)
  (app/render (app/current-screen)))

(defn resize [w h]
  (.update graphics/gui-viewport   w h true)
  (.update graphics/world-viewport w h))

(ns cdq.ctx.create.graphics
  (:require [cdq.graphics.impl :as graphics]
            [cdq.files :as files-utils])
  (:import (com.badlogic.gdx Files)))

(defn- handle-files
  [files {:keys [colors
                 cursors
                 default-font
                 tile-size
                 texture-folder
                 ui-viewport
                 world-viewport]}]
  {:ui-viewport ui-viewport
   :default-font {:file-handle (Files/.internal files (:path default-font))
                  :params (:params default-font)}
   :colors colors
   :cursors (update-vals (:data cursors)
                         (fn [[short-path hotspot]]
                           [(Files/.internal files (format (:path-format cursors) short-path))
                            hotspot]))
   :world-unit-scale (float (/ tile-size))
   :world-viewport world-viewport
   :textures-to-load (files-utils/search files texture-folder)})

(defn do!
  [{:keys [ctx/gdx]
    :as ctx}
   params]
  (assoc ctx :ctx/graphics (let [{:keys [clojure.gdx/graphics
                                         clojure.gdx/files]} gdx]
                             (graphics/create! (handle-files files params)
                                               graphics))))

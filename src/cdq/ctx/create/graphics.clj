(ns cdq.ctx.create.graphics
  (:require [cdq.graphics.impl :as graphics]
            [clojure.gdx.files :as files]
            [cdq.files :as files-utils]))

(defn- handle-files
  [files {:keys [colors
                 cursors
                 default-font
                 tile-size
                 texture-folder
                 ui-viewport
                 world-viewport]}]
  {:ui-viewport ui-viewport
   :default-font {:file-handle (files/internal files (:path default-font))
                  :params (:params default-font)}
   :colors colors
   :cursors (update-vals (:data cursors)
                         (fn [[short-path hotspot]]
                           [(files/internal files (format (:path-format cursors) short-path))
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

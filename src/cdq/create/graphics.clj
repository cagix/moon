(ns cdq.create.graphics
  (:require [cdq.files]
            [cdq.gdx.graphics]
            [clojure.gdx.files :as files]))

(defn graphics-config
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
   :textures-to-load (cdq.files/search files texture-folder)})

(defn do! [{:keys [ctx/config
                   ctx/draw-fns
                   ctx/gdx]
            :as ctx}]
  (assoc ctx :ctx/graphics (let [{:keys [clojure.gdx/files
                                         clojure.gdx/graphics]} gdx]
                             (assoc (cdq.gdx.graphics/create graphics
                                                             (graphics-config files
                                                                              (:after-gdx-create config)))
                                    :ctx/draw-fns draw-fns))))

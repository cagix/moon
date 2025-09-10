(ns cdq.create.graphics
  (:require [cdq.files]
            [cdq.gdx.graphics]
            [clojure.gdx :as gdx]
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

(defn do! [ctx]
  (assoc ctx :ctx/graphics (assoc (cdq.gdx.graphics/create (gdx/graphics)
                                                           (graphics-config (:ctx/files ctx)
                                                                            (:after-gdx-create (:ctx/config ctx))))
                                  :ctx/draw-fns (:ctx/draw-fns ctx))))

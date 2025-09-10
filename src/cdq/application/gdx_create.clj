(ns cdq.application.gdx-create
  (:require [cdq.files]
            [cdq.gdx.graphics]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.vis-ui :as vis-ui]))

(defn after-gdx-create!
  [ctx]
  (vis-ui/load! (:cdq.vis-ui (:ctx/config ctx)))
  (let [files (gdx/files)
        {:keys [ui-viewport
                default-font
                colors
                cursors
                tile-size
                world-viewport
                texture-folder]} (:after-gdx-create (:ctx/config ctx))
        graphics-config {:ui-viewport ui-viewport
                         :default-font {:file-handle (files/internal files (:path default-font))
                                        :params (:params default-font)}
                         :colors colors
                         :cursors (update-vals (:data cursors)
                                               (fn [[short-path hotspot]]
                                                 [(files/internal files (format (:path-format cursors) short-path))
                                                  hotspot]))
                         :world-unit-scale (float (/ tile-size))
                         :world-viewport world-viewport
                         :textures-to-load (cdq.files/search files texture-folder)}
        graphics (cdq.gdx.graphics/create (gdx/graphics) graphics-config)
        input (gdx/input)
        stage (scene2d/stage (:ctx/ui-viewport graphics)
                             (:ctx/batch       graphics))]
    (input/set-processor! input stage)
    (merge ctx
           {:ctx/graphics (assoc graphics :ctx/draw-fns (:ctx/draw-fns ctx))
            :ctx/input input
            :ctx/stage stage
            :ctx/audio (let [{:keys [sound-names
                                     path-format]} (:cdq.audio/config (:ctx/config ctx))]
                         (into {}
                               (for [sound-name (->> sound-names io/resource slurp edn/read-string)
                                     :let [path (format path-format sound-name)]]
                                 [sound-name
                                  (audio/sound (gdx/audio) (files/internal files path))])))})))

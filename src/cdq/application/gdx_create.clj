(ns cdq.application.gdx-create
  (:require [cdq.files]
            [cdq.gdx.graphics]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.gdx.graphics.camera :as camera]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.vis-ui :as vis-ui]))

(def sounds "sounds.edn")
(def sound-path-format "sounds/%s.wav")

(def ui-viewport-width 1440)
(def ui-viewport-height 900)

(defn after-gdx-create!
  [{:keys [ctx/world-unit-scale]
    :as ctx}]
  (vis-ui/load! {:skin-scale :x1})
  (let [graphics (cdq.gdx.graphics/create
                  (gdx/graphics)
                  {
                   :world-unit-scale world-unit-scale
                   :textures-to-load (cdq.files/search (gdx/files)
                                                       {:folder "resources/"
                                                        :extensions #{"png" "bmp"}})
                   })
        ui-viewport (viewport/fit ui-viewport-width ui-viewport-height (camera/orthographic))
        input (gdx/input)
        stage (scene2d/stage ui-viewport (:ctx/batch graphics))]
    (input/set-processor! input stage)
    (merge ctx
           graphics
           {:ctx/ui-viewport ui-viewport
            :ctx/input input
            :ctx/stage stage
            :ctx/audio (into {}
                             (for [sound-name (->> sounds io/resource slurp edn/read-string)
                                   :let [path (format sound-path-format sound-name)]]
                               [sound-name
                                (audio/sound (gdx/audio) (files/internal (gdx/files) path))]))})))

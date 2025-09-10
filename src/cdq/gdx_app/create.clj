(ns cdq.gdx-app.create
  (:require cdq.application.reset-game-state
            [cdq.ctx :as ctx]
            [cdq.files]
            [cdq.gdx.graphics]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.gdx :as gdx]
            [clojure.gdx.audio :as audio]
            [clojure.gdx.files :as files]
            [clojure.gdx.input :as input]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.vis-ui :as vis-ui]))

(defn load-vis-ui [ctx]
  (vis-ui/load! (:cdq.vis-ui (:ctx/config ctx)))
  ctx)

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

(defn load-graphics [ctx]
  (assoc ctx :ctx/graphics (assoc (cdq.gdx.graphics/create (gdx/graphics)
                                                           (graphics-config (:ctx/files ctx)
                                                                            (:after-gdx-create (:ctx/config ctx))))
                                  :ctx/draw-fns (:ctx/draw-fns ctx))))

(defn create-stage [ctx]
  (assoc ctx :ctx/stage (scene2d/stage (:ctx/ui-viewport (:ctx/graphics ctx))
                                       (:ctx/batch       (:ctx/graphics ctx)))))

(defn set-input-processor! [ctx]
  (input/set-processor! (:ctx/input ctx)
                        (:ctx/stage ctx))
  ctx)

(defn create-audio [ctx]
  (assoc ctx :ctx/audio (let [{:keys [sound-names
                                      path-format]} (:cdq.audio/config (:ctx/config ctx))]
                          (into {}
                                (for [sound-name (->> sound-names io/resource slurp edn/read-string)
                                      :let [path (format path-format sound-name)]]
                                  [sound-name
                                   (audio/sound (gdx/audio) (files/internal (:ctx/files ctx) path))])))))

(defn do! [ctx]
  (assert (:ctx/db ctx))
  (-> ctx
      (assoc :ctx/input (gdx/input))
      (assoc :ctx/files (gdx/files))
      load-vis-ui
      load-graphics
      create-stage
      set-input-processor!
      create-audio
      (dissoc :ctx/files)
      ctx/reset-stage!
      (cdq.application.reset-game-state/reset-game-state! (:starting-world (:cdq.gdx-app.create (:ctx/config ctx))))))

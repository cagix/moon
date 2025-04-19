(ns cdq.create.stage
  (:require [clojure.gdx.scenes.scene2d.group :as group])
  (:import (com.badlogic.gdx Gdx)
           (com.kotcrab.vis.ui VisUI VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip)
           (gdl StageWithState)))

(def config
  '{:skin-scale :x1})

(defn- create-stage [viewport batch]
  (proxy [StageWithState clojure.lang.ILookup] [viewport batch]
    (valAt
      ([id]
       (group/find-actor-with-id (StageWithState/.getRoot this) id))
      ([id not-found]
       (or (group/find-actor-with-id (StageWithState/.getRoot this) id)
           not-found)))))

(defn create [batch viewport]
  ; app crashes during startup before VisUI/dispose and we do cdq.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case (:skin-scale config)
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0))
  (let [stage (create-stage viewport batch)]
    (.setInputProcessor Gdx/input stage)
    stage))

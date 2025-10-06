(ns cdq.ui.stage
  (:import (com.badlogic.gdx.scenes.scene2d CtxStage)))

(defn create [viewport batch]
  (CtxStage. viewport batch))

(defprotocol Stage
  (set-ctx! [_ ctx])
  (get-ctx [_])
  (act! [_])
  (draw! [_])
  (add! [_ actor])
  (clear! [_])
  (root [_])
  (hit [_ [x y]])
  (viewport [_]))

(extend-type CtxStage
  Stage
  (set-ctx! [stage ctx]
    (set! (.ctx stage) ctx))

  (get-ctx [stage]
    (.ctx stage))

  (act! [stage]
    (.act stage))

  (draw! [stage]
    (.draw stage))

  (add! [stage actor]
    (.addActor stage actor))

  (clear! [stage]
    (.clear stage))

  (root [stage]
    (.getRoot stage))

  (hit [stage [x y]]
    (.hit stage x y true))

  (viewport [stage]
    (.getViewport stage)))

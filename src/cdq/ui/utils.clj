(ns cdq.ui.utils
  (:import (cdq.ui CtxStage)
           (com.badlogic.gdx.scenes.scene2d InputEvent)
           (com.badlogic.gdx.scenes.scene2d.utils ChangeListener)))

(defn change-listener ^ChangeListener [on-clicked]
  (proxy [ChangeListener] []
    (changed [event actor]
      (on-clicked actor @(.ctx ^CtxStage (InputEvent/.getStage event))))))

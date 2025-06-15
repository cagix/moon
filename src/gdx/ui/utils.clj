(ns gdx.ui.utils
  (:import (com.badlogic.gdx.scenes.scene2d.utils ClickListener)
           (gdl.ui CtxStage)))

(defn click-listener [f]
  (proxy [ClickListener] []
    (clicked [event _x _y]
      (f @(.ctx ^CtxStage (.getStage event))))))

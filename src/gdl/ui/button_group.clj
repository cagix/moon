(ns gdl.ui.button-group
  (:import (com.badlogic.gdx.scenes.scene2d.ui Button
                                               ButtonGroup)))

(defn create [{:keys [max-check-count min-check-count]}]
  (doto (ButtonGroup.)
    (.setMaxCheckCount max-check-count)
    (.setMinCheckCount min-check-count)))

(defprotocol BG
  (checked [_])
  (add! [_ button])
  (remove! [_ button]))

(extend-type ButtonGroup
  BG
  (checked [button-group]
    (.getChecked button-group))

  (add! [button-group button]
    (.add button-group ^Button button))

  (remove! [button-group button]
    (.remove button-group ^Button button)))


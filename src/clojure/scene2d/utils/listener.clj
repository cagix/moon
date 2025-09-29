(ns clojure.scene2d.utils.listener ; => move to actor protocol
  (:import (com.badlogic.gdx.scenes.scene2d.utils ChangeListener
                                                  ClickListener)))

(defn change [f]
  (proxy [ChangeListener] []
    (changed [event actor]
      (f event actor))))

(defn click [f]
  (proxy [ClickListener] []
    (clicked [event x y]
      (f event x y))))

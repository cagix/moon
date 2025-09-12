(ns clojure.gdx.scene2d.utils
  (:import (com.badlogic.gdx.scenes.scene2d.utils ChangeListener
                                                  ClickListener)))

(defn change-listener [f]
  (proxy [ChangeListener] []
    (changed [event actor]
      (f event actor))))

(defn click-listener [f]
  (proxy [ClickListener] []
    (clicked [event x y]
      (f event x y))))

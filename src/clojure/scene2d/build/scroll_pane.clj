(ns clojure.scene2d.build.scroll-pane
  (:require [clojure.gdx.vis-ui.widget.vis-scroll-pane :as vis-scroll-pane]
            [clojure.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (vis-scroll-pane/create actor
                                {:flick-scroll? false
                                 :fade-scroll-bars? false})
    (actor/set-name! name)))

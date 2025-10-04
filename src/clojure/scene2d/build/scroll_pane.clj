(ns clojure.scene2d.build.scroll-pane
  (:require [clojure.gdx.scenes.scene2d.vis-ui.widget.vis-scroll-pane :as vis-scroll-pane]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.actor :as actor]))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (vis-scroll-pane/create actor
                                {:flick-scroll? false
                                 :fade-scroll-bars? false})
    (clojure.scene2d.actor/set-name! name)))

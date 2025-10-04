(ns gdl.scene2d.build.scroll-pane
  (:require [com.kotcrab.vis.ui.widget.vis-scroll-pane :as vis-scroll-pane]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.actor :as actor]))

(defmethod scene2d/build :actor.type/scroll-pane
  [{:keys [scroll-pane/actor
           actor/name]}]
  (doto (vis-scroll-pane/create actor
                                {:flick-scroll? false
                                 :fade-scroll-bars? false})
    (gdl.scene2d.actor/set-name! name)))

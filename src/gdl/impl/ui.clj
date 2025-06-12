(ns gdl.impl.ui
  (:require [clojure.gdx :as gdx]
            [clojure.gdx.vis-ui :as vis-ui]
            [gdl.ui :as ui]
            [gdl.ui.stage])
  (:import (gdl.ui CtxStage)))

(extend-type gdl.ui.CtxStage
  gdl.ui.stage/Stage
  (render! [stage ctx]
    (reset! (.ctx stage) ctx)
    (.act stage)
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system
    #_@(.ctx (-k ctx))
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; FIXME so it has outdated context.
    #_(reset! (.ctx (-k ctx)) nil)
    (reset! (.ctx stage) ctx)
    (.draw stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx (-k ctx)) nil)
    ctx)

  (add! [stage actor] ; -> re-use gdl.ui/add! ?
    (ui/add! stage actor))

  (clear! [stage]
    (.clear stage))

  (hit [stage position]
    (ui/hit stage position))

  (find-actor [stage actor-name]
    (-> stage
        .getRoot
        (ui/find-actor actor-name))))

(extend-type com.badlogic.gdx.scenes.scene2d.Group
  gdl.ui/PGroup
  (find-actor [group name]
    (.findActor group name))
  (clear-children! [group]
    (.clearChildren group))
  (children [group]
    (.getChildren group)))

; => these functions here are _private_
; they actually belong in 'clojure.gdx.scenes.scene2d.actor' as public functions API
; then we extend them here
; this can be also automated...
(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  gdl.ui/PActor
  (get-x [actor]
    (.getX actor))

  (get-y [actor]
    (.getY actor))

  (get-name [actor]
    (.getName actor))

  (user-object [actor]
    (.getUserObject actor))

  (set-user-object! [actor object]
    (.setUserObject actor object))

  (visible? [actor]
    (.isVisible actor))

  (set-visible! [actor visible?]
    (.setVisible actor visible?))

  (set-touchable! [actor touchable]
    (.setTouchable actor (case touchable
                           :disabled com.badlogic.gdx.scenes.scene2d.Touchable/disabled)))

  (remove! [actor]
    (.remove actor))

  (parent [actor]
    (.getParent actor)))

(defn create! [user-interface graphics]
  (vis-ui/load! user-interface)
  (let [stage (proxy [CtxStage clojure.lang.ILookup] [(:ui-viewport graphics)
                                                      (:batch graphics)
                                                      (atom nil)]
                (valAt [id]
                  (ui/find-actor-with-id (CtxStage/.getRoot this) id)))]
    (gdx/set-input-processor! stage)
    stage))

(ns cdq.create.stage
  (:require [gdl.c :as c]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx Gdx)))

(def ^:private -k :ctx/stage)

(defn add-stage! [ctx]
  {:pre [(nil? (-k ctx))]}
  (let [stage (ui/stage (:java-object (:ctx/ui-viewport ctx))
                        (:java-object (:ctx/batch       ctx)))]
    (.setInputProcessor Gdx/input stage)
    (assoc ctx -k stage)))

(extend-type gdl.application.Context
  c/Stage
  (draw-stage! [ctx]
    (reset! (.ctx (-k ctx)) ctx)
    (ui/draw! (-k ctx))
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx (-k ctx)) nil)
    nil)

  (update-stage! [ctx]
    (reset! (.ctx (-k ctx)) ctx)
    (ui/act! (-k ctx))
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system:
    ;                         com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>   Lwjgl3Application.java:  153
    ;                           com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop   Lwjgl3Application.java:  181
    ;                              com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window.update        Lwjgl3Window.java:  414
    ;                        com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input.update  DefaultLwjgl3Input.java:  190
    ;                                            com.badlogic.gdx.InputEventQueue.drain     InputEventQueue.java:   70
    ;                             gdl.ui.proxy$gdl.ui.CtxStage$ILookup$a65747ce.touchUp                         :
    ;                                     com.badlogic.gdx.scenes.scene2d.Stage.touchUp               Stage.java:  354
    ;                              com.badlogic.gdx.scenes.scene2d.InputListener.handle       InputListener.java:   71
    #_@(.ctx (-k ctx))
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    #_(reset! (.ctx (-k ctx)) nil)
    nil)

  (get-actor [ctx id]
    (id (-k ctx)))

  (find-actor-by-name [ctx name]
    (-> (-k ctx)
        ui/root
        (ui/find-actor name)))

  (add-actor! [ctx actor]
    (ui/add! (-k ctx) actor))

  (mouseover-actor [ctx]
    (ui/hit (-k ctx) (c/ui-mouse-position ctx)))

  (reset-actors! [ctx actors]
    (ui/clear! (-k ctx))
    (run! #(ui/add! (-k ctx) %) actors)))

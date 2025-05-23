(ns cdq.render.update-ui
  (:require [gdl.ui :as ui]))

(defn do! [{:keys [ctx/stage] :as ctx}]
  (reset! (.ctx stage) ctx)
  (ui/act! stage)
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
  #_@(.ctx stage)
  ; we need to set nil as input listeners
  ; are updated outside of render
  ; inside lwjgl3application code
  ; so it has outdated context
  #_(reset! (.ctx stage) nil)
  nil
  )

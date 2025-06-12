(ns gdl.create.ui
  (:require [clojure.gdx :as gdx]
            [gdl.ui :as ui]
            [gdl.ui.stage])
  (:import (com.badlogic.gdx.math Vector2)
           (com.badlogic.gdx.utils Align)
           (com.kotcrab.vis.ui VisUI
                               VisUI$SkinScale)
           (com.kotcrab.vis.ui.widget Tooltip
                                      VisLabel)
           (gdl.ui CtxStage)))

(defn- load-vis-ui! [{:keys [skin-scale]}]
  ; app crashes during startup before VisUI/dispose and we do clojure.tools.namespace.refresh-> gui elements not showing.
  ; => actually there is a deeper issue at play
  ; we need to dispose ALL resources which were loaded already ...
  (when (VisUI/isLoaded)
    (VisUI/dispose))
  (VisUI/load (case skin-scale
                :x1 VisUI$SkinScale/X1
                :x2 VisUI$SkinScale/X2))
  (-> (VisUI/getSkin)
      (.getFont "default-font")
      .getData
      .markupEnabled
      (set! true))
  ;(set! Tooltip/DEFAULT_FADE_TIME (float 0.3))
  ;Controls whether to fade out tooltip when mouse was moved. (default false)
  ;(set! Tooltip/MOUSE_MOVED_FADEOUT true)
  (set! Tooltip/DEFAULT_APPEAR_DELAY_TIME (float 0)))

(defn do! [{:keys [ctx/graphics] :as ctx} params]
  (load-vis-ui! params)
  (let [stage (proxy [CtxStage clojure.lang.ILookup] [(:ui-viewport graphics)
                                                      (:batch graphics)
                                                      (atom nil)]
                (valAt [id]
                  (ui/find-actor-with-id (CtxStage/.getRoot this) id)))]
    (gdx/set-input-processor! stage)
    stage))

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

(extend-type com.badlogic.gdx.scenes.scene2d.Actor
  gdl.ui/PActorTooltips
  (add-tooltip! [actor tooltip-text]
    (let [text? (string? tooltip-text)
          label (VisLabel. (if text? tooltip-text ""))
          tooltip (proxy [Tooltip] []
                    ; hooking into getWidth because at
                    ; https://github.com/kotcrab/vis-blob/master/ui/src/main/java/com/kotcrab/vis/ui/widget/Tooltip.java#L271
                    ; when tooltip position gets calculated we setText (which calls pack) before that
                    ; so that the size is correct for the newly calculated text.
                    (getWidth []
                      (let [^Tooltip this this]
                        (when-not text?
                          (let [actor (.getTarget this)
                                ctx (ui/get-stage-ctx actor)]
                            (when ctx ; ctx is only set later for update!/draw! ... not at starting of initialisation
                              (.setText this (str (tooltip-text ctx))))))
                        (proxy-super getWidth))))]
      (.setAlignment label Align/center)
      (.setTarget  tooltip actor)
      (.setContent tooltip label))
    actor)

  (remove-tooltip! [actor]
    (Tooltip/removeTooltip actor)))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  gdl.ui/PTable
  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         ; this is weird now as actor declarations are all maps ....
         (map? props-or-actor) (-> (ui/add! table (:actor props-or-actor))
                                   (ui/set-cell-opts! (dissoc props-or-actor :actor)))
         :else (ui/add! table props-or-actor)))
      (.row table))
    table))

(extend-protocol gdl.ui/CanAddActor
  com.badlogic.gdx.scenes.scene2d.Group
  (add! [group actor]
    (.addActor group (ui/-create-actor actor)))

  com.badlogic.gdx.scenes.scene2d.Stage
  (add! [stage actor]
    (.addActor stage (ui/-create-actor actor)))

  com.badlogic.gdx.scenes.scene2d.ui.Table
  (add! [table actor]
    (.add table (ui/-create-actor actor))))

(extend-protocol gdl.ui/CanHit
  com.badlogic.gdx.scenes.scene2d.Actor
  (hit [actor [x y]]
    (let [v (.stageToLocalCoordinates actor (Vector2. x y))]
      (.hit actor (.x v) (.y v) true)))

  com.badlogic.gdx.scenes.scene2d.Stage
  (hit [stage [x y]]
    (.hit stage x y true)))

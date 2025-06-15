(ns gdl.create.ui
  (:require [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.group :as group]
            [gdl.ui.stage]
            [gdl.ui.table]
            [gdx.ui]
            [gdx.ui.table.cell :as cell])
  (:import (gdl.ui CtxStage)))

(defn do! [{:keys [ctx/graphics
                   ctx/input] :as ctx} params]
  (gdx.ui/load! params)
  (let [stage (gdx.ui/stage (:ui-viewport graphics)
                            (:batch       graphics))]
    (input/set-processor! input stage)
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

  (add! [stage actor]
    (ui/add! stage actor))

  (clear! [stage]
    (.clear stage))

  (hit [stage position]
    (ui/hit stage position))

  (find-actor [stage actor-name]
    (-> stage
        .getRoot
        (group/find-actor actor-name))))

(extend-type com.badlogic.gdx.scenes.scene2d.ui.Table
  gdl.ui.table/Table
  (add-rows! [table rows]
    (doseq [row rows]
      (doseq [props-or-actor row]
        (cond
         ; this is weird now as actor declarations are all maps ....
         (map? props-or-actor) (-> (ui/add! table (:actor props-or-actor))
                                   (cell/set-opts! (dissoc props-or-actor :actor)))
         :else (ui/add! table props-or-actor)))
      (.row table))
    table))

(extend-protocol gdl.ui/CanAddActor
  com.badlogic.gdx.scenes.scene2d.Stage
  (add! [stage actor]
    (.addActor stage (gdx.ui.actor/construct? actor)))

  com.badlogic.gdx.scenes.scene2d.ui.Table
  (add! [table actor]
    (.add table (gdx.ui.actor/construct? actor))))

(extend-protocol gdl.ui/CanHit
  com.badlogic.gdx.scenes.scene2d.Stage
  (hit [stage [x y]]
    (.hit stage x y true)))

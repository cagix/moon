(ns gdl.create.ui
  (:require [gdl.input :as input]
            [gdl.ui.group :as group]
            [gdl.ui.stage]
            [gdx.ui])
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
    (.addActor stage (gdx.ui.actor/construct? actor)))

  (clear! [stage]
    (.clear stage))

  (hit [stage [x y]]
    (.hit stage x y true))

  (find-actor [stage actor-name]
    (-> stage
        .getRoot
        (group/find-actor actor-name))))

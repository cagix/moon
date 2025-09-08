(ns cdq.start.entity-states
  (:require cdq.entity.state
            cdq.entity.state.create
            cdq.entity.state.enter
            cdq.entity.state.cursor
            cdq.entity.state.exit
            cdq.entity.state.handle-input))

(defn do! [ctx]
  (.bindRoot #'cdq.entity.state/->create            cdq.entity.state.create/function-map)
  (.bindRoot #'cdq.entity.state/state->enter        cdq.entity.state.enter/function-map)
  (.bindRoot #'cdq.entity.state/state->cursor       cdq.entity.state.cursor/function-map)
  (.bindRoot #'cdq.entity.state/state->exit         cdq.entity.state.exit/function-map)
  (.bindRoot #'cdq.entity.state/state->handle-input cdq.entity.state.handle-input/function-map)
  ctx)

(ns eu.stratuslab.cimi.vmm.one
  (:require [clojure.tools.logging :as log]))

(defn create
  "Create a new virtual machine with the parameters passed in the
  template.  Returns the machine's identifier (vmid)."
  [template]
  (log/info "creating virtual machine with" template)
  "fake-uuid")

(defn summary
  "Provides a concise summary of the user's active virtual machines.
  This is all machines that haven't been deleted."
  []
  (log/info "list all virtual machines")
  {"a" "ok", "b" "bad"})

(defn status
  "Provides the detailed status information for a single virtual
  machine."
  [vmid]
  (log/info "status for virtual machine" vmid)
  {:state :unknown})

(defn update
  "Updates the virtual machine's state.  Used to move the machine
  between various states, such as running, suspended, stopped, etc.
  May also be used to changed dynamic hardware configurations if
  supported by the VMM and to set 'watches' on state changes."
  [data]
  (if-let [vmid (:vmid data)]
    (log/info "updating virtual machine" vmid)
    (log/info "missing vmid for update")))

(defn delete
  "Kill and delete a virtual machine.  This immediately halts the
  machine (if running) and releases all allocated resources."
  [vmid]
  (log/info "killing virtual machine" vmid))

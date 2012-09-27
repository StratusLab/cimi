(ns eu.stratuslab.authn.vm-rest.vmm.dummy
  "Dummy implementation of a virtual machine manager backend.  This is
   used to test and verify the application logic."
  (:require [eu.stratuslab.authn.vm-rest.utils :as utils]
            [clojure.tools.logging :as log]))

(def vm-database (atom {}))

(defn create
  "Create a new virtual machine with the parameters passed in the
  template.  Returns the machine's identifier (vmid).  The vmid is an
  opaque handle to the backend's identifier."
  [template]
  (let [vmid (utils/create-uuid)
        template (assoc template
                   :vmid vmid
                   :state :created)]
    (swap! vm-database assoc vmid template)
    (log/info "created vm" vmid "with" template)
    vmid))

(defn summary
  "Provides a concise summary of the user's active virtual machines.
  This is all machines that haven't been deleted."
  []
  (log/info "vm list contains" (count @vm-database) "vms")
  @vm-database)

(defn status
  "Provides the detailed status information for a single virtual
  machine."
  [vmid]
  (log/info "provided status for vm" vmid)
  (get @vm-database vmid))

(defn update
  "Updates the virtual machine's state.  Used to move the machine
  between various states, such as running, suspended, stopped, etc.
  May also be used to changed dynamic hardware configurations if
  supported by the VMM and to set 'watches' on state changes."
  [data]
  (if-let [vmid (:vmid data)]
    (do 
      (log/info "updating virtual machine" vmid)
      (swap! vm-database assoc vmid data))
    (log/info "missing vmid for update")))

(defn delete
  "Kill and delete a virtual machine.  This immediately halts the
  machine (if running) and releases all allocated resources."
  [vmid]
  (log/info "killing virtual machine" vmid)
  (swap! vm-database dissoc vmid))

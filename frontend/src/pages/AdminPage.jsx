import { useState } from 'react';
import Header from '@/common/components/Header';
import './AdminPage.css';
import CrawlerManagement from '@/features/admin/components/CrawlerManagement.jsx';
import AdminTabs from '@/features/admin/components/AdminTabs.jsx';
import CatalogManagement from '@/features/admin/components/CatalogManagement.jsx';
import UserManagement from '@/features/admin/components/UserManagement.jsx';
import BrandCategoryFormModal from '@/features/admin/components/BrandCategoryFormModal.jsx';
import UserSuspensionModal from '@/features/admin/components/UserSuspensionModal.jsx';
import ConfirmActionModal from '@/features/admin/components/ConfirmActionModal.jsx';
import { useAdminCatalog } from '@/features/admin/hooks/useAdminCatalog.js';
import { useAdminUsers } from '@/features/admin/hooks/useAdminUsers.js';
import { useAdminCrawler } from '@/features/admin/hooks/useAdminCrawler.js';

export default function AdminPage({ user, onBack }) {
  const [tab, setTab] = useState('brand');

  const {
      brands,
      categories,
      loading,
      message,
      setMessage,
      loadLists,
      form,
      setForm,
      confirmDelete,
      setConfirmDelete,
      deleteItem,
      toggleBrandActive,
      confirmDeleteItem,
      openCreate,
      openEdit,
      toggleCategory,
      updateFormField,
      submitForm,
    confirmTextMap
  } = useAdminCatalog();

  const {
      selectedUser,
      userNickname,
      setUserNickname,
      userComments,
      commentsLoading,
      userMessage,
      suspendModal,
      suspendError,
      suspendSubmitting,
      findUser,
      openSuspendModal,
      closeSuspendModal,
      handleSuspendDaysChange,
      handleSuspendReasonChange,
      submitSuspension,
  } = useAdminUsers();

  const { crawlerLoading, runCrawler } = useAdminCrawler(setMessage);

  if (user?.role !== 'ADMIN') {
      return (
          <div className="admin-denied">
              <h1>접근 권한이 없습니다.</h1>
              <button type="button" onClick={onBack}>메인으로</button>
          </div>
      );
  }

  const confirmText = confirmDelete
      ? confirmTextMap[confirmDelete.kind ?? 'delete']
      : null;

  return (
      <div className="admin-page">
          <Header />
          <main className="admin-container">
              <button type="button" className="admin-back" onClick={onBack}>
                  ← 메인으로
              </button>

              <div className="admin-page-title">
                  <div>
                      <span>ADMINISTRATION</span>
                      <h1>관리자 페이지</h1>
                      <p>브랜드, 카테고리, 회원 정보를 관리합니다.</p>
                  </div>
                  <button type="button" onClick={loadLists}>새로고침</button>
              </div>

              <AdminTabs tab={tab} onChange={setTab} />

              {message && <p className="admin-message" role="status">{message}</p>}

              {loading ? (
                  <p className="admin-empty">목록을 불러오는 중입니다...</p>
              ) : tab === 'user' ? (
                  <UserManagement
                      userNickname={userNickname}
                      onNicknameChange={setUserNickname}
                      onSearch={findUser}
                      selectedUser={selectedUser}
                      userMessage={userMessage}
                      userComments={userComments}
                      commentsLoading={commentsLoading}
                      onSuspend={() => openSuspendModal(false)}
                      onRelease={() => openSuspendModal(true)}
                  />
              ) : tab === 'crawler' ? (
                  <CrawlerManagement
                      crawlerLoading={crawlerLoading}
                      onRunCrawler={runCrawler}
                  />
              ) : (
                  <CatalogManagement
                      tab={tab}
                      brands={brands}
                      categories={categories}
                      onCreate={openCreate}
                      onEdit={openEdit}
                      onToggleBrandActive={toggleBrandActive}
                      onDelete={deleteItem}
                  />
              )}

              <BrandCategoryFormModal
                  form={form}
                  categories={categories}
                  onClose={() => setForm(null)}
                  onSubmit={submitForm}
                  onToggleCategory={toggleCategory}
                  onNameChange={(event) => updateFormField('name', event.target.value)}
                  onUrlChange={(event) => updateFormField('url', event.target.value)}
                  onImageChange={(event) => updateFormField('img', event.target.value)}
              />

              <UserSuspensionModal
                  suspendModal={suspendModal}
                  selectedUser={selectedUser}
                  suspendError={suspendError}
                  suspendSubmitting={suspendSubmitting}
                  onClose={closeSuspendModal}
                  onSubmit={submitSuspension}
                  onDaysChange={handleSuspendDaysChange}
                  onReasonChange={handleSuspendReasonChange}
              />

              <ConfirmActionModal
                  confirmDelete={confirmDelete}
                  confirmText={confirmText}
                  onClose={() => setConfirmDelete(null)}
                  onConfirm={confirmDeleteItem}
              />
          </main>
      </div>
  );
}